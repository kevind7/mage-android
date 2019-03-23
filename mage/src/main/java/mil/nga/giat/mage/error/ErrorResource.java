package mil.nga.giat.mage.error;

import android.content.Context;
import android.preference.PreferenceManager;

import com.google.gson.JsonObject;

import java.io.PrintWriter;
import java.io.StringWriter;

import mil.nga.giat.mage.sdk.http.HttpClientManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

public class ErrorResource {

    public interface ErrorService {
        @POST("/api/errors")
        Call<JsonObject> createError(@Body JsonObject error);
    }

    private Context context;

    public ErrorResource(Context context) {
        this.context = context;
    }

    public void createError(String message, Exception e, Callback<JsonObject> callback) {
        String baseUrl = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(mil.nga.giat.mage.sdk.R.string.serverURLKey), context.getString(mil.nga.giat.mage.sdk.R.string.serverURLDefaultValue));
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(HttpClientManager.getInstance(context).httpClient())
                .build();

        ErrorService service = retrofit.create(ErrorService.class);

        JsonObject json = new JsonObject();
        json.addProperty("message", message);

        StringWriter exceptionStringWriter = new StringWriter();
        PrintWriter exceptionWriter = new PrintWriter(exceptionStringWriter);
        e.printStackTrace(exceptionWriter);
        json.addProperty("exception", exceptionStringWriter.toString());

        Throwable cause = e.getCause();
        if (cause != null) {
            StringWriter causeStringWriter = new StringWriter();
            PrintWriter causeWriter = new PrintWriter(causeStringWriter);
            cause.printStackTrace(causeWriter);
            json.addProperty("cause", causeStringWriter.toString());
        }

        service.createError(json).enqueue(callback);
    }
}
