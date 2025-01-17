package test.mega.privacy.android.app.presentation.photos.albums

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.R
import mega.privacy.android.app.domain.usecase.GetNodeListByIds
import mega.privacy.android.app.presentation.photos.albums.AlbumsViewModel
import mega.privacy.android.app.presentation.photos.albums.model.UIAlbum
import mega.privacy.android.app.presentation.photos.albums.model.mapper.UIAlbumMapper
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.entity.photos.PhotoPredicate
import mega.privacy.android.domain.usecase.CreateAlbum
import mega.privacy.android.domain.usecase.GetAlbumPhotos
import mega.privacy.android.domain.usecase.GetDefaultAlbumPhotos
import mega.privacy.android.domain.usecase.GetDefaultAlbumsMap
import mega.privacy.android.domain.usecase.GetFeatureFlagValue
import mega.privacy.android.domain.usecase.GetUserAlbums
import mega.privacy.android.domain.usecase.RemoveAlbums
import mega.privacy.android.domain.usecase.RemoveFavourites
import mega.privacy.android.domain.usecase.RemovePhotosFromAlbumUseCase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull

@ExperimentalCoroutinesApi
class AlbumsViewModelTest {
    private lateinit var underTest: AlbumsViewModel

    private val getDefaultAlbumPhotos = mock<GetDefaultAlbumPhotos>()
    private val uiAlbumMapper = mock<UIAlbumMapper>()
    private val getUserAlbums = mock<GetUserAlbums>()
    private val getAlbumPhotos = mock<GetAlbumPhotos>()
    private val getFeatureFlag =
        mock<GetFeatureFlagValue> { onBlocking { invoke(any()) }.thenReturn(true) }
    private val getDefaultAlbumsMap = mock<GetDefaultAlbumsMap>()
    private val removeFavourites = mock<RemoveFavourites>()
    private val getNodeListByIds = mock<GetNodeListByIds>()
    private val createAlbum = mock<CreateAlbum>()
    private val removeAlbums = mock<RemoveAlbums>()
    private val removePhotosFromAlbumUseCase = mock<RemovePhotosFromAlbumUseCase>()
    private val proscribedStrings =
        listOf("My albums", "Shared albums", "Favourites", "RAW", "GIFs")

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())

        underTest = AlbumsViewModel(
            getDefaultAlbumPhotos = getDefaultAlbumPhotos,
            getDefaultAlbumsMap = getDefaultAlbumsMap,
            getUserAlbums = getUserAlbums,
            getAlbumPhotos = getAlbumPhotos,
            uiAlbumMapper = uiAlbumMapper,
            getFeatureFlag = getFeatureFlag,
            removeFavourites = removeFavourites,
            getNodeListByIds = getNodeListByIds,
            createAlbum = createAlbum,
            removeAlbums = removeAlbums,
            removePhotosFromAlbumUseCase = removePhotosFromAlbumUseCase,
            defaultDispatcher = UnconfinedTestDispatcher(),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        underTest.state.test {
            val initial = awaitItem()
            assertThat(initial.albums).isEmpty()
        }
    }

    @Test
    fun `test that an error would return an empty list`() = runTest {
        whenever(getDefaultAlbumPhotos(listOf())).thenReturn(flow { throw Exception("Error") })

        underTest.state.test {
            assertEquals(emptyList(), awaitItem().albums)
        }
    }

    @Test
    fun `test that returned albums are added to the state if there are photos for them`() =
        runTest {
            val defaultAlbums: Map<Album, PhotoPredicate> = mapOf(
                Album.FavouriteAlbum to { true },
                Album.GifAlbum to { true },
                Album.RawAlbum to { true },
            )

            whenever(uiAlbumMapper(any(), eq(Album.FavouriteAlbum))).thenReturn(
                UIAlbum(
                    title = "Favourite",
                    count = 0,
                    coverPhoto = null,
                    photos = emptyList(),
                    id = Album.FavouriteAlbum
                )
            )
            whenever(uiAlbumMapper(any(), eq(Album.GifAlbum))).thenReturn(
                UIAlbum(
                    title = "GIFs",
                    count = 0,
                    coverPhoto = null,
                    photos = emptyList(),
                    id = Album.GifAlbum
                )
            )
            whenever(uiAlbumMapper(any(), eq(Album.RawAlbum))).thenReturn(
                UIAlbum(
                    title = "RAW",
                    count = 0,
                    coverPhoto = null,
                    photos = emptyList(),
                    id = Album.RawAlbum
                )
            )

            whenever(getDefaultAlbumsMap()).thenReturn(defaultAlbums)

            whenever(getDefaultAlbumPhotos(any())).thenReturn(flowOf(listOf(createImage())))

            underTest.state.drop(1).test {
                assertThat(awaitItem().albums.map { it.id }).containsExactlyElementsIn(defaultAlbums.keys)
            }
        }

    @Test
    fun `test that albums are not added, if there are no photos in them`() = runTest {
        val defaultAlbums: Map<Album, PhotoPredicate> = mapOf(
            Album.FavouriteAlbum to { true },
            Album.GifAlbum to { true },
            Album.RawAlbum to { false },
        )

        whenever(uiAlbumMapper(any(), eq(Album.FavouriteAlbum))).thenReturn(
            UIAlbum(
                title = "Favourites",
                count = 0,
                coverPhoto = null,
                photos = emptyList(),
                id = Album.FavouriteAlbum
            )
        )
        whenever(uiAlbumMapper(any(), eq(Album.GifAlbum))).thenReturn(
            UIAlbum(
                title = "GIFs",
                count = 0,
                coverPhoto = null,
                photos = emptyList(),
                id = Album.GifAlbum
            )
        )

        whenever(getDefaultAlbumsMap()).thenReturn(defaultAlbums)

        whenever(getDefaultAlbumPhotos(any())).thenReturn(flowOf(listOf(createImage())))

        underTest.state.drop(1).test {
            assertThat(awaitItem().albums.map { it.id })
                .containsExactlyElementsIn(defaultAlbums.keys.filterNot { it == Album.RawAlbum })
        }
    }

    @Test
    fun `test that favourite album is displayed even if it contains no photos`() = runTest {
        val defaultAlbums: Map<Album, PhotoPredicate> = mapOf(
            Album.FavouriteAlbum to { false },
            Album.GifAlbum to { false },
            Album.RawAlbum to { false },
        )

        whenever(uiAlbumMapper(any(), eq(Album.FavouriteAlbum))).thenReturn(
            UIAlbum(
                title = "Favourites",
                count = 0,
                coverPhoto = null,
                photos = emptyList(),
                id = Album.FavouriteAlbum
            )
        )

        whenever(getDefaultAlbumsMap()).thenReturn(defaultAlbums)

        whenever(getDefaultAlbumPhotos(any())).thenReturn(flowOf(emptyList()))

        underTest.state.drop(1).test {
            assertThat(awaitItem().albums.map { it.id })
                .containsExactlyElementsIn(defaultAlbums.keys.filter { it == Album.FavouriteAlbum })
        }
    }

    @Test
    fun `test that feature flag filters out the correct albums`() = runTest {
        val defaultAlbums: Map<Album, PhotoPredicate> = mapOf(
            Album.FavouriteAlbum to { true },
            Album.GifAlbum to { true },
            Album.RawAlbum to { true },
        )

        whenever(uiAlbumMapper(any(), eq(Album.FavouriteAlbum))).thenReturn(
            UIAlbum(
                title = "Favourites",
                count = 0,
                coverPhoto = null,
                photos = emptyList(),
                id = Album.FavouriteAlbum
            )
        )
        whenever(uiAlbumMapper(any(), eq(Album.GifAlbum))).thenReturn(
            UIAlbum(
                title = "GIFs",
                count = 0,
                coverPhoto = null,
                photos = emptyList(),
                id = Album.GifAlbum
            )
        )
        whenever(uiAlbumMapper(any(), eq(Album.RawAlbum))).thenReturn(
            UIAlbum(
                title = "RAW",
                count = 0,
                coverPhoto = null,
                photos = emptyList(),
                id = Album.RawAlbum
            )
        )

        whenever(getFeatureFlag(any())).thenReturn(false)

        whenever(getDefaultAlbumsMap()).thenReturn(defaultAlbums)

        whenever(getDefaultAlbumPhotos(any())).thenReturn(flowOf(emptyList()))

        underTest.state.drop(1).test {
            val albums = awaitItem().albums
            assertEquals(albums.size, 1)
            assertThat(albums.first().id).isEqualTo(Album.FavouriteAlbum)
        }
    }

    @Test
    fun `test that album is using latest modification time photo`() = runTest {
        val defaultAlbums: Map<Album, PhotoPredicate> = mapOf(
            Album.FavouriteAlbum to { true },
            Album.GifAlbum to { false },
            Album.RawAlbum to { false },
        )

        val testPhotosList = listOf(
            createImage(id = 1L, modificationTime = LocalDateTime.MAX),
            createImage(id = 2L, modificationTime = LocalDateTime.MIN)
        )

        whenever(uiAlbumMapper(testPhotosList, Album.FavouriteAlbum)).thenReturn(
            UIAlbum(
                title = "Favourites",
                count = testPhotosList.size,
                coverPhoto = createImage(id = 1L, modificationTime = LocalDateTime.MAX),
                photos = testPhotosList,
                id = Album.FavouriteAlbum,
            )
        )

        whenever(getDefaultAlbumsMap()).thenReturn(defaultAlbums)

        whenever(getDefaultAlbumPhotos(any())).thenReturn(flowOf(testPhotosList))

        underTest.state.drop(1).test {
            assertThat(awaitItem().albums.map { it.coverPhoto?.id }.firstOrNull()).isEqualTo(1L)
        }
    }

    @Test
    fun `test that user albums collect is working and the sorting order is correct`() = runTest {
        val newAlbum1 =
            createUserAlbum(id = AlbumId(1L), title = "Album 1", modificationTime = 100L)
        val newAlbum2 =
            createUserAlbum(id = AlbumId(2L), title = "Album 2", modificationTime = 200L)
        val newAlbum3 =
            createUserAlbum(id = AlbumId(3L), title = "Album 3", modificationTime = 300L)

        whenever(uiAlbumMapper(any(), eq(newAlbum1))).thenReturn(
            UIAlbum(
                title = newAlbum1.title,
                count = 0,
                coverPhoto = newAlbum1.cover,
                photos = emptyList(),
                id = newAlbum1,
            )
        )

        whenever(uiAlbumMapper(any(), eq(newAlbum2))).thenReturn(
            UIAlbum(
                title = newAlbum2.title,
                count = 0,
                coverPhoto = newAlbum2.cover,
                photos = emptyList(),
                id = newAlbum2,
            )
        )

        whenever(uiAlbumMapper(any(), eq(newAlbum3))).thenReturn(
            UIAlbum(
                title = newAlbum3.title,
                count = 0,
                coverPhoto = newAlbum3.cover,
                photos = emptyList(),
                id = newAlbum3,
            )
        )

        whenever(getUserAlbums()).thenReturn(flowOf(listOf(
            newAlbum1, newAlbum2, newAlbum3
        )))
        whenever(getAlbumPhotos(AlbumId(any()))).thenReturn(flowOf(listOf()))

        val expectedUserAlbums = listOf(
            createUserAlbum(id = AlbumId(3L), title = "Album 3", modificationTime = 300L),
            createUserAlbum(id = AlbumId(2L), title = "Album 2", modificationTime = 200L),
            createUserAlbum(id = AlbumId(1L), title = "Album 1", modificationTime = 100L),
        )

        underTest.state.drop(1).test {
            val actualUserAlbums = awaitItem().albums.map { it.id }
            assertThat(expectedUserAlbums).isEqualTo(actualUserAlbums)
        }
    }

    @Test
    fun `test that create album returns an album with the right name`() = runTest {
        val expectedAlbumName = "Album 1"

        whenever(createAlbum(expectedAlbumName)).thenReturn(
            createUserAlbum(title = expectedAlbumName)
        )

        underTest.createNewAlbum(expectedAlbumName, proscribedStrings)

        underTest.state.drop(1).test {
            val actualAlbum = awaitItem().currentAlbum as Album.UserAlbum
            assertEquals(expectedAlbumName, actualAlbum.title)
        }
    }

    @Test
    fun `test that an error in creating an album would keep current album as null`() = runTest {
        whenever(createAlbum(any())).thenAnswer { throw Exception() }

        underTest.createNewAlbum("ABD", proscribedStrings)

        underTest.state.test {
            assertNull(awaitItem().currentAlbum)
        }
    }

    @Test
    fun `test that setPlaceholderAlbumTitle set the right text if no album with default name exists`() =
        runTest {
            val expectedName = "New album"
            whenever(getUserAlbums()).thenReturn(flowOf(listOf()))

            underTest.setPlaceholderAlbumTitle("New album")

            underTest.state.test {
                val actualName = awaitItem().createAlbumPlaceholderTitle
                assertEquals(expectedName, actualName)
            }
        }

    @Test
    fun `test that setPlaceholderAlbumTitle set the right text if an album with default name already exists`() =
        runTest {
            val expectedName = "New album(1)"
            val newUserAlbum = createUserAlbum(title = "New album")
            whenever(uiAlbumMapper(any(), eq(newUserAlbum))).thenReturn(
                UIAlbum(
                    title = newUserAlbum.title,
                    count = 0,
                    coverPhoto = newUserAlbum.cover,
                    photos = emptyList(),
                    id = newUserAlbum,
                )
            )
            whenever(getUserAlbums()).thenReturn(flowOf(listOf(
                newUserAlbum
            )))
            whenever(getAlbumPhotos(AlbumId(any()))).thenReturn(flowOf(listOf()))


            underTest.state.drop(1).test {
                awaitItem()
                underTest.setPlaceholderAlbumTitle("New album")
                val actualName = awaitItem().createAlbumPlaceholderTitle
                assertEquals(expectedName, actualName)
            }
        }

    @Test
    fun `test that setPlaceholderAlbumTitle set the right text if two albums with default name already exist`() =
        runTest {
            val expectedName = "New album(2)"
            val newAlbum1 =
                createUserAlbum(id = AlbumId(1L), title = "New album", modificationTime = 1L)
            val newAlbum2 =
                createUserAlbum(id = AlbumId(2L), title = "New album(1)", modificationTime = 2L)

            whenever(uiAlbumMapper(any(), eq(newAlbum1))).thenReturn(
                UIAlbum(
                    title = newAlbum1.title,
                    count = 0,
                    coverPhoto = newAlbum1.cover,
                    photos = emptyList(),
                    id = newAlbum1,
                )
            )

            whenever(uiAlbumMapper(any(), eq(newAlbum2))).thenReturn(
                UIAlbum(
                    title = newAlbum2.title,
                    count = 0,
                    coverPhoto = newAlbum2.cover,
                    photos = emptyList(),
                    id = newAlbum2,
                )
            )

            whenever(getUserAlbums()).thenReturn(flowOf(listOf(
                newAlbum1,
                newAlbum2
            )))
            whenever(getAlbumPhotos(AlbumId(any()))).thenReturn(flowOf(listOf()))


            underTest.state.drop(1).test {
                awaitItem()
                underTest.setPlaceholderAlbumTitle("New album")
                val actualName = awaitItem().createAlbumPlaceholderTitle
                assertEquals(expectedName, actualName)
            }
        }

    @Test
    fun `test that creating an album with a system album title will not create the album`() =
        runTest {
            val defaultAlbums: Map<Album, PhotoPredicate> = mapOf(
                Album.FavouriteAlbum to { true },
                Album.GifAlbum to { false },
                Album.RawAlbum to { false },
            )

            whenever(uiAlbumMapper(any(), eq(Album.FavouriteAlbum))).thenReturn(
                UIAlbum(
                    title = "Favourites",
                    count = 0,
                    coverPhoto = null,
                    photos = emptyList(),
                    id = Album.FavouriteAlbum
                )
            )

            whenever(getDefaultAlbumsMap()).thenReturn(defaultAlbums)
            whenever(getDefaultAlbumPhotos(any())).thenReturn(flowOf(emptyList()))

            underTest.state.drop(1).test {
                awaitItem()
                underTest.createNewAlbum("favourites", proscribedStrings)
                val item = awaitItem()
                assertEquals(false, item.isInputNameValid)
                assertEquals(
                    R.string.photos_create_album_error_message_systems_album,
                    item.createDialogErrorMessage
                )
            }
            verifyNoInteractions(createAlbum)

        }

    @Test
    fun `test that creating an album with an empty system album title will not create the album`() =
        runTest {
            val defaultAlbums: Map<Album, PhotoPredicate> = mapOf(
                Album.FavouriteAlbum to { true },
                Album.GifAlbum to { false },
                Album.RawAlbum to { false },
            )

            whenever(uiAlbumMapper(any(), eq(Album.FavouriteAlbum))).thenReturn(
                UIAlbum(
                    title = "Favourites",
                    count = 0,
                    coverPhoto = null,
                    photos = emptyList(),
                    id = Album.FavouriteAlbum
                )
            )

            whenever(getDefaultAlbumsMap()).thenReturn(defaultAlbums)
            whenever(getDefaultAlbumPhotos(any())).thenReturn(flowOf(emptyList()))

            underTest.state.drop(1).test {
                awaitItem()
                underTest.createNewAlbum("raw", proscribedStrings)
                val item = awaitItem()
                assertEquals(false, item.isInputNameValid)
                assertEquals(
                    R.string.photos_create_album_error_message_systems_album,
                    item.createDialogErrorMessage
                )
            }
            verifyNoInteractions(createAlbum)

        }

    @Test
    fun `test that creating an album with all spaces will not create the album`() =
        runTest {
            underTest.state.test {
                awaitItem()
                underTest.createNewAlbum("      ", proscribedStrings)
                val item = awaitItem()
                assertEquals(false, item.isInputNameValid)
                assertEquals(
                    R.string.photos_create_album_error_message_systems_album,
                    item.createDialogErrorMessage
                )
            }
            verifyNoInteractions(createAlbum)
        }

    @Test
    fun `test that creating an album with an existing title will not create the album`() =
        runTest {
            val testAlbumName = "Album 1"
            val newAlbum1 = createUserAlbum(title = testAlbumName)

            whenever(uiAlbumMapper(any(), eq(newAlbum1))).thenReturn(
                UIAlbum(
                    title = newAlbum1.title,
                    count = 0,
                    coverPhoto = newAlbum1.cover,
                    photos = emptyList(),
                    id = newAlbum1,
                )
            )

            whenever(getUserAlbums()).thenReturn(flowOf(listOf(newAlbum1)))
            whenever(getAlbumPhotos(AlbumId(any()))).thenReturn(flowOf(listOf()))

            underTest.state.drop(1).test {
                awaitItem()
                underTest.createNewAlbum(testAlbumName, proscribedStrings)
                val item = awaitItem()
                assertEquals(false, item.isInputNameValid)
                assertEquals(
                    item.createDialogErrorMessage,
                    R.string.photos_create_album_error_message_duplicate
                )
            }
            verifyNoInteractions(createAlbum)
        }

    @Test
    fun `test that creating an album with an invalid character will not create the album`() =
        runTest {
            val testAlbumName = "*"

            underTest.state.test {
                awaitItem()
                underTest.createNewAlbum(testAlbumName, proscribedStrings)
                val item = awaitItem()
                assertEquals(false, item.isInputNameValid)
                assertEquals(
                    item.createDialogErrorMessage,
                    R.string.invalid_characters_defined
                )
            }

            verifyNoInteractions(createAlbum)
        }

    @Test
    fun `test that albums are deleted properly`() = runTest {
        whenever(removeAlbums(any())).thenReturn(Unit)

        val expectedDeletedAlbumIds = setOf(
            AlbumId(1L),
            AlbumId(2L),
            AlbumId(3L),
        )
        underTest.deleteAlbums(expectedDeletedAlbumIds.toList())

        underTest.state.test {
            val actualDeletedAlbumIds = awaitItem().deletedAlbumIds
            assertEquals(expectedDeletedAlbumIds, actualDeletedAlbumIds)
        }
    }

    @Test
    fun `test that album deleted message is updated properly`() = runTest {
        val expectedMessage = "Album deleted"
        underTest.updateAlbumDeletedMessage(expectedMessage)

        underTest.state.test {
            val actualMessage = awaitItem().albumDeletedMessage
            assertEquals(expectedMessage, actualMessage)
        }
    }

    @Test
    fun `test that delete album confirmation is shown properly`() = runTest {
        underTest.showDeleteAlbumsConfirmation()
        underTest.state.test {
            val showDeleteAlbumsConfirmation = awaitItem().showDeleteAlbumsConfirmation
            assertThat(showDeleteAlbumsConfirmation).isTrue()
        }
    }

    @Test
    fun `test that delete album confirmation is closed properly`() = runTest {
        underTest.closeDeleteAlbumsConfirmation()
        underTest.state.test {
            val showDeleteAlbumsConfirmation = awaitItem().showDeleteAlbumsConfirmation
            assertThat(showDeleteAlbumsConfirmation).isFalse()
        }
    }

    @Test
    fun `test that album is selected properly`() = runTest {
        // given
        val expectedAlbum = Album.UserAlbum(
            id = AlbumId(1L),
            title = "Album 1",
            cover = null,
            modificationTime = 0L,
        )

        // when
        underTest.selectAlbum(expectedAlbum)

        // then
        underTest.state.drop(1).test {
            val selectedAlbumIds = awaitItem().selectedAlbumIds
            assertThat(expectedAlbum.id in selectedAlbumIds).isTrue()
        }
    }

    @Test
    fun `test that album is unselected properly`() = runTest {
        // given
        val expectedAlbum = Album.UserAlbum(
            id = AlbumId(1L),
            title = "Album 1",
            cover = null,
            modificationTime = 0L,
        )

        // when
        underTest.selectAlbum(expectedAlbum)
        underTest.unselectAlbum(expectedAlbum)

        // then
        underTest.state.test {
            val selectedAlbumIds = awaitItem().selectedAlbumIds
            assertThat(expectedAlbum.id !in selectedAlbumIds).isTrue()
        }
    }

    @Test
    fun `test that album is cleared properly`() = runTest {
        // given
        val expectedAlbum1 = Album.UserAlbum(
            id = AlbumId(1L),
            title = "Album 1",
            cover = null,
            modificationTime = 0L,
        )

        val expectedAlbum2 = Album.UserAlbum(
            id = AlbumId(2L),
            title = "Album 2",
            cover = null,
            modificationTime = 0L,
        )

        // when
        underTest.selectAlbum(expectedAlbum1)
        underTest.selectAlbum(expectedAlbum2)
        underTest.clearAlbumSelection()

        // then
        underTest.state.test {
            val selectedAlbumIds = awaitItem().selectedAlbumIds
            assertThat(selectedAlbumIds.isEmpty()).isTrue()
        }
    }

    private fun createUserAlbum(
        id: AlbumId = AlbumId(0L),
        title: String = "",
        cover: Photo? = null,
        modificationTime: Long = 0L,
    ): Album.UserAlbum = Album.UserAlbum(id, title, cover, modificationTime)

    private fun createImage(
        id: Long = 2L,
        parentId: Long = 0L,
        isFavourite: Boolean = false,
        modificationTime: LocalDateTime = LocalDateTime.now(),
        fileTypeInfo: FileTypeInfo = StaticImageFileTypeInfo("", ""),
    ): Photo = Photo.Image(
        id = id,
        parentId = parentId,
        name = "",
        isFavourite = isFavourite,
        creationTime = LocalDateTime.now(),
        modificationTime = modificationTime,
        thumbnailFilePath = "thumbnailFilePath",
        previewFilePath = "previewFilePath",
        fileTypeInfo = fileTypeInfo
    )
}
