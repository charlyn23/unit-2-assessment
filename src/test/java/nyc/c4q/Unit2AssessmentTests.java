package nyc.c4q;

import android.Manifest;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.robolectric.AndroidManifest;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowToast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import nyc.c4q.model.FlickrResponse;
import nyc.c4q.rest.APIManager;
import nyc.c4q.rest.FlickrService;
import nyc.c4q.rest.MockFlickrService;
import retrofit.Callback;
import retrofit.MockRestAdapter;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Header;
import retrofit.client.Response;
import retrofit.converter.ConversionException;

import static junit.framework.Assert.fail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(RobolectricTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", emulateSdk = 18)
public class Unit2AssessmentTests {

    @Mock
    private FlickrService flickrService;

    @Captor
    ArgumentCaptor<Callback<FlickrResponse>> actionCaptor;

    @Before
    public void setUp() {
        initMocks(this);
        // this serializes execution for unit tests
        Executor syncExecutor = new SynchronousExecutor();

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(APIManager.API_URL)
                .setExecutors(syncExecutor, syncExecutor)
                .build();

        MockRestAdapter mockRestAdapter = MockRestAdapter.from(restAdapter);
        MockFlickrService mockFlickrService = new MockFlickrService();

        flickrService = mockRestAdapter.create(FlickrService.class, mockFlickrService);
    }

    @Test
    public void getOnMainCrashes() throws IOException {
        try {
            flickrService.getInterestingPhotos(Mockito.anyInt(), Mockito.anyInt(), new Callback<FlickrResponse>() {
                @Override
                public void success(FlickrResponse flickrResponse, Response response) {

                }

                @Override
                public void failure(RetrofitError error) {

                }
            });
            fail("Calling getInterestingPhotos() on main thread should throw exception");
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    public void appHasInternetPermission() {
        AndroidManifest manifest = Robolectric.getShadowApplication().getAppManifest();
        List<String> usedPermissions = manifest.getUsedPermissions();

        assertThat(usedPermissions).contains(Manifest.permission.INTERNET);
    }

    @Test
    public void ApiKeyExists() {
        // TODO
    }

    @Test
    public void shouldReturnSomePhotos() {
        flickrService.getInterestingPhotos(Mockito.anyInt(), MockFlickrService.PAGE_0, new Callback<FlickrResponse>() {
            @Override
            public void success(FlickrResponse flickrResponse, Response response) {
                assertThat(flickrResponse.mPhotosInfo.mPhotos).isNotEmpty();
            }

            @Override
            public void failure(RetrofitError error) {
                fail(error.toString());
            }
        });
    }

    @Test
    public void shouldReturnNoPhotos() {
        flickrService.getInterestingPhotos(Mockito.anyInt(), MockFlickrService.PAGE_2, new Callback<FlickrResponse>() {
            @Override
            public void success(FlickrResponse flickrResponse, Response response) {
                assertThat(flickrResponse.mPhotosInfo.mPhotos).isEmpty();
            }

            @Override
            public void failure(RetrofitError error) {
                fail(error.toString());
            }
        });
    }

    @Test
    public void getToastsIfNetworkError() {
        Mockito.verify(flickrService).getInterestingPhotos(Mockito.anyInt(), Mockito.anyInt(), actionCaptor.capture());
        RetrofitError error = RetrofitError.networkError(null, new IOException("Network Error"));
        actionCaptor.getValue().failure(error);
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo("Network Error");
    }

    @Test
    public void getToastsIfHttpError() {
        Mockito.verify(flickrService).getInterestingPhotos(Mockito.anyInt(), Mockito.anyInt(), actionCaptor.capture());
        RetrofitError error = RetrofitError.httpError(null,
                new Response("", 404, "Page not found", new ArrayList<Header>(), null), null, null);
        actionCaptor.getValue().failure(error);
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo("Http Error");
    }

    @Test
    public void getToastsIfConversionError() {
        Mockito.verify(flickrService).getInterestingPhotos(Mockito.anyInt(), Mockito.anyInt(), actionCaptor.capture());
        RetrofitError error = RetrofitError.conversionError(null, null, null, null, new ConversionException("Conversion Error"));
        actionCaptor.getValue().failure(error);
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo("Conversion Error");
    }

    @Test
    public void getRethrowsUnexpectedError() throws Exception {
        try {
            Mockito.verify(flickrService).getInterestingPhotos(Mockito.anyInt(), Mockito.anyInt(), actionCaptor.capture());
            RetrofitError error = RetrofitError.unexpectedError(null, new Exception("Unknown Error"));
            actionCaptor.getValue().failure(error);
            fail("An UnexpectedError should be rethrown, to crash the program");
        } catch (RetrofitError ignored) {
        }
    }

    private class SynchronousExecutor implements Executor {
        @Override
        public void execute(Runnable command) {
            command.run();
        }
    }
}
