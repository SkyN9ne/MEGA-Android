package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.constant.CacheFolderConstant
import mega.privacy.android.data.extensions.getPreviewFileName
import mega.privacy.android.data.extensions.getThumbnailFileName
import mega.privacy.android.data.gateway.CacheFolderGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.FileTypeInfoMapper
import mega.privacy.android.data.mapper.ImageMapper
import mega.privacy.android.data.mapper.NodeUpdateMapper
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.mapper.VideoMapper
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.data.wrapper.DateUtilWrapper
import mega.privacy.android.domain.entity.GifFileTypeInfo
import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.RawFileTypeInfo
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.photos.AlbumPhotoId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.PhotosRepository
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaNode
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Default implementation of [PhotosRepository]
 *
 * @property megaApiFacade MegaApiGateway
 * @property ioDispatcher CoroutineDispatcher
 * @property cacheFolderFacade CacheFolderGateway
 * @property megaLocalStorageFacade MegaLocalStorageGateway
 * @property imageMapper ImageMapper
 * @property videoMapper VideoMapper
 * @property nodeUpdateMapper NodeUpdateMapper
 */
internal class DefaultPhotosRepository @Inject constructor(
    private val megaApiFacade: MegaApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val cacheFolderFacade: CacheFolderGateway,
    private val megaLocalStorageFacade: MegaLocalStorageGateway,
    private val dateUtilFacade: DateUtilWrapper,
    private val imageMapper: ImageMapper,
    private val videoMapper: VideoMapper,
    private val nodeUpdateMapper: NodeUpdateMapper,
    private val fileTypeInfoMapper: FileTypeInfoMapper,
    private val sortOrderIntMapper: SortOrderIntMapper,
) : PhotosRepository {

    private var thumbnailFolderPath: String? = null

    private var previewFolderPath: String? = null

    override suspend fun getPublicLinksCount(): Int = withContext(ioDispatcher) {
        megaApiFacade.getPublicLinks().size
    }

    override suspend fun buildDefaultDownloadDir(): File = withContext(ioDispatcher) {
        cacheFolderFacade.buildDefaultDownloadDir()
    }

    override suspend fun getCameraUploadFolderId(): Long? = withContext(ioDispatcher) {
        val getCameraUploadFolderId = megaLocalStorageFacade.getCamSyncHandle()
        getCameraUploadFolderId
    }

    override suspend fun getMediaUploadFolderId(): Long? = withContext(ioDispatcher) {
        val getMediaUploadFolderId = megaLocalStorageFacade.getMegaHandleSecondaryFolder()
        getMediaUploadFolderId
    }

    override fun monitorNodeUpdates(): Flow<NodeUpdate> =
        megaApiFacade.globalUpdates
            .filterIsInstance<GlobalUpdate.OnNodesUpdate>()
            .mapNotNull {
                it.nodeList?.toList()
            }.map { nodeList ->
                nodeUpdateMapper(nodeList)
            }

    override suspend fun searchMegaPhotos(): List<Photo> = withContext(ioDispatcher) {
        val images = async { mapPhotoNodesToImages(searchImages()) }
        val videos = async { mapPhotoNodesToVideos(searchVideos()) }
        images.await() + videos.await()
    }

    override suspend fun getPhotoFromNodeID(nodeId: NodeId, albumPhotoId: AlbumPhotoId?): Photo? =
        withContext(ioDispatcher) {
            megaApiFacade.getMegaNodeByHandle(nodeHandle = nodeId.longValue)
                ?.let { megaNode ->
                    megaNode to fileTypeInfoMapper(megaNode)
                }?.let { (megaNode, fileType) ->
                    when (fileType) {
                        is StaticImageFileTypeInfo, is GifFileTypeInfo, is RawFileTypeInfo -> {
                            mapMegaNodeToImage(megaNode, albumPhotoId?.id)
                        }
                        is VideoFileTypeInfo -> {
                            mapMegaNodeToVideo(megaNode, albumPhotoId?.id)
                        }
                        else -> {
                            null
                        }
                    }
                }
        }

    override suspend fun getPhotosByFolderId(id: Long, order: SortOrder): List<Photo> =
        withContext(ioDispatcher) {
            val parent = megaApiFacade.getMegaNodeByHandle(id)
            parent?.let { parentNode ->
                val megaNodes = megaApiFacade.getChildren(
                    parent = parentNode,
                    order = sortOrderIntMapper(order),
                )
                mapMegaNodesToPhotos(megaNodes)
            } ?: emptyList()
        }

    private suspend fun searchImages(): List<MegaNode> = withContext(ioDispatcher) {
        val token = MegaCancelToken.createInstance()
        val imageNodes = megaApiFacade.searchByType(
            token,
            MegaApiAndroid.ORDER_MODIFICATION_DESC,
            MegaApiAndroid.FILE_TYPE_PHOTO,
            MegaApiAndroid.SEARCH_TARGET_ROOTNODE
        )
        suspendCancellableCoroutine { continuation ->
            continuation.resumeWith(Result.success(imageNodes))
            continuation.invokeOnCancellation {
                token.cancel()
            }
        }
        token.cancel()
        imageNodes
    }

    private suspend fun searchVideos(): List<MegaNode> = withContext(ioDispatcher) {
        val token = MegaCancelToken.createInstance()
        val videosNodes = megaApiFacade.searchByType(
            token,
            MegaApiAndroid.ORDER_MODIFICATION_DESC,
            MegaApiAndroid.FILE_TYPE_VIDEO,
            MegaApiAndroid.SEARCH_TARGET_ROOTNODE
        )
        suspendCancellableCoroutine { continuation ->
            continuation.resumeWith(Result.success(videosNodes))
            continuation.invokeOnCancellation {
                token.cancel()
            }
        }
        token.cancel()
        videosNodes
    }

    /**
     * Map megaNodes to Photos.
     */
    private suspend fun mapMegaNodesToPhotos(megaNodes: List<MegaNode>): List<Photo> {
        return megaNodes.mapNotNull { megaNode ->
            val fileType = fileTypeInfoMapper(megaNode)
            val isValid =
                megaNode.isFile
                        && (fileType is VideoFileTypeInfo || fileType is ImageFileTypeInfo)
                        && !megaApiFacade.isInRubbish(megaNode)
            if (isValid.not()) return@mapNotNull null
            if (fileType is ImageFileTypeInfo) {
                mapMegaNodeToImage(megaNode)
            } else {
                mapMegaNodeToVideo(megaNode)
            }
        }
    }

    /**
     * Convert the Photos MegaNode list to Image list
     * @param megaNodes List<MegaNode> of Photo
     * @return List<Photo> / Images
     */
    private suspend fun mapPhotoNodesToImages(megaNodes: List<MegaNode>): List<Photo> {
        return megaNodes.filter {
            it.isValidPhotoNode()
        }.map { megaNode ->
            mapMegaNodeToImage(megaNode)
        }
    }

    /**
     * Convert the Photos MegaNode list to Video list
     * @param megaNodes List<MegaNode> of Photo
     * @return List<Photo> / Videos
     */
    private suspend fun mapPhotoNodesToVideos(megaNodes: List<MegaNode>): List<Photo> {
        return megaNodes.filter {
            it.isValidPhotoNode()
        }.map { megaNode ->
            mapMegaNodeToVideo(megaNode)
        }
    }

    /**
     * Check valid Photo Node, not include Photo nodes that are in rubbish bin or without thumbnail
     */
    private suspend fun MegaNode.isValidPhotoNode() =
        !megaApiFacade.isInRubbish(this) && this.hasThumbnail()

    /**
     * Convert the MegaNode to Image
     * @param megaNode MegaNode
     * @return Photo / Image
     */
    private fun mapMegaNodeToImage(megaNode: MegaNode, albumPhotoId: Long? = null) =
        imageMapper(
            megaNode.handle,
            albumPhotoId,
            megaNode.parentHandle,
            megaNode.name,
            megaNode.isFavourite,
            dateUtilFacade.fromEpoch(megaNode.creationTime),
            dateUtilFacade.fromEpoch(megaNode.modificationTime),
            getThumbnailCacheFilePath(megaNode),
            getPreviewCacheFilePath(megaNode),
            fileTypeInfoMapper(megaNode),
        )

    /**
     * Convert the MegaNode to Video
     * @param megaNode MegaNode
     * @return Photo / Video
     */
    private fun mapMegaNodeToVideo(megaNode: MegaNode, albumPhotoId: Long? = null) =
        videoMapper(
            megaNode.handle,
            albumPhotoId,
            megaNode.parentHandle,
            megaNode.name,
            megaNode.isFavourite,
            dateUtilFacade.fromEpoch(megaNode.creationTime),
            dateUtilFacade.fromEpoch(megaNode.modificationTime),
            getThumbnailCacheFilePath(megaNode),
            getPreviewCacheFilePath(megaNode),
            fileTypeInfoMapper(megaNode),
        )

    private fun getThumbnailCacheFilePath(megaNode: MegaNode): String? {
        if (thumbnailFolderPath == null) {
            thumbnailFolderPath =
                cacheFolderFacade.getCacheFolder(CacheFolderConstant.THUMBNAIL_FOLDER)?.path
        }
        return thumbnailFolderPath?.let {
            "$it${File.separator}${megaNode.getThumbnailFileName()}"
        }
    }

    private fun getPreviewCacheFilePath(megaNode: MegaNode): String? {
        if (previewFolderPath == null) {
            previewFolderPath =
                cacheFolderFacade.getCacheFolder(CacheFolderConstant.PREVIEW_FOLDER)?.path
        }
        return previewFolderPath?.let {
            "$it${File.separator}${megaNode.getPreviewFileName()}"
        }
    }
}
