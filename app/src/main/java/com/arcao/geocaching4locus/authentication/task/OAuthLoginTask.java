package com.arcao.geocaching4locus.authentication.task;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.arcao.geocaching.api.GeocachingApi;
import com.arcao.geocaching.api.GeocachingApiFactory;
import com.arcao.geocaching.api.data.UserProfile;
import com.arcao.geocaching.api.data.apilimits.ApiLimitsResponse;
import com.arcao.geocaching.api.exception.InvalidResponseException;
import com.arcao.geocaching.api.exception.NetworkException;
import com.arcao.geocaching.api.oauth.GeocachingOAuthProvider;
import com.arcao.geocaching4locus.App;
import com.arcao.geocaching4locus.BuildConfig;
import com.arcao.geocaching4locus.authentication.util.Account;
import com.arcao.geocaching4locus.authentication.util.AccountManager;
import com.arcao.geocaching4locus.authentication.util.AccountRestrictions;
import com.arcao.geocaching4locus.authentication.util.DeviceInfoFactory;
import com.arcao.geocaching4locus.authentication.util.OAuthAsyncRequestCallbackAdapter;
import com.arcao.geocaching4locus.base.constants.AppConstants;
import com.arcao.geocaching4locus.base.task.UserTask;
import com.arcao.geocaching4locus.error.handler.ExceptionHandler;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuth1RequestToken;
import com.github.scribejava.core.oauth.OAuth10aService;
import com.github.scribejava.httpclient.okhttp.OkHttpHttpClient;

import java.lang.ref.WeakReference;

import timber.log.Timber;

public class OAuthLoginTask extends UserTask<String, Void, String[]> {
    public interface TaskListener {
        void onLoginUrlAvailable(@NonNull String url);

        void onTaskFinished(@Nullable Intent errorIntent);
    }

    private final Context context;
    private final WeakReference<TaskListener> taskListenerRef;
    Throwable tokenRequestThrowable;

    public OAuthLoginTask(Context context, TaskListener listener) {
        this.context = context.getApplicationContext();
        taskListenerRef = new WeakReference<>(listener);
    }

    private OAuth10aService createOAuthService() {
        ServiceBuilder serviceBuilder = new ServiceBuilder(BuildConfig.GEOCACHING_API_KEY)
                .apiSecret(BuildConfig.GEOCACHING_API_SECRET)
                .callback(AppConstants.OAUTH_CALLBACK_URL)
                .httpClient(new OkHttpHttpClient(GeocachingApiFactory.getOkHttpClient()));

        if (BuildConfig.DEBUG) {
            serviceBuilder.debug();
        }

        if (BuildConfig.GEOCACHING_API_STAGING) {
            return serviceBuilder.build(new GeocachingOAuthProvider.Staging());
        } else {
            return serviceBuilder.build(new GeocachingOAuthProvider());
        }
    }

    @Override
    protected String[] doInBackground(String... params) throws Exception {
        AccountManager accountManager = App.get(context).getAccountManager();
        AccountRestrictions accountRestrictions = accountManager.getRestrictions();

        OAuth10aService service = createOAuthService();

        if (params.length == 0) {
            OAuth1RequestToken requestToken = service.getRequestTokenAsync(new OAuthAsyncRequestCallbackAdapter<OAuth1RequestToken>() {
                @Override
                public void onThrowable(Throwable t) {
                    tokenRequestThrowable = t;
                }
            }).get();

            if (tokenRequestThrowable != null)
                throw new NetworkException(tokenRequestThrowable.getMessage(), tokenRequestThrowable);

            accountManager.setOAuthRequestToken(requestToken);
            String authUrl = service.getAuthorizationUrl(requestToken);
            Timber.i("AuthorizationUrl: %s", authUrl);
            return new String[]{authUrl};
        } else {
            OAuth1RequestToken requestToken = accountManager.getOAuthRequestToken();
            OAuth1AccessToken accessToken = service.getAccessTokenAsync(requestToken, params[0], new OAuthAsyncRequestCallbackAdapter<OAuth1AccessToken>() {
                @Override
                public void onThrowable(Throwable t) {
                    tokenRequestThrowable = t;
                }
            }).get();

            if (tokenRequestThrowable != null)
                throw new NetworkException(tokenRequestThrowable.getMessage(), tokenRequestThrowable);

            // get account name
            GeocachingApi api = GeocachingApiFactory.create();
            api.openSession(accessToken.getToken());

            UserProfile userProfile = api.getYourUserProfile(false, false, false, false, false, false, DeviceInfoFactory.create(context));
            ApiLimitsResponse apiLimitsResponse = api.getApiLimits();

            if (userProfile == null)
                throw new InvalidResponseException("User profile is null");

            Account account = accountManager.createAccount(userProfile.user());
            accountManager.addAccount(account);
            accountManager.setOAuthToken(api.getSession());
            accountManager.deleteOAuthRequestToken();

            // update restrictions
            accountRestrictions.updateLimits(apiLimitsResponse.apiLimits());

            return null;
        }
    }

    @Override
    protected void onPostExecute(String[] result) {
        TaskListener listener = taskListenerRef.get();

        if (listener == null)
            return;

        if (result != null && result.length == 1) {
            listener.onLoginUrlAvailable(result[0]);
        } else {
            listener.onTaskFinished(null);
        }
    }

    @Override
    protected void onException(Throwable t) {
        super.onException(t);

        if (isCancelled())
            return;

        Timber.e(t);

        Intent intent = new ExceptionHandler(context).handle(t);

        TaskListener listener = taskListenerRef.get();
        if (listener != null) listener.onTaskFinished(intent);
    }
}
