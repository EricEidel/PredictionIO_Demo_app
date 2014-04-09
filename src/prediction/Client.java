package prediction;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ning.http.client.*;
import com.ning.http.client.extra.ThrottleRequestFilter;
import com.ning.http.client.providers.netty.NettyAsyncHttpProvider;

import java.io.Closeable;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * The Client class is a full feature abstraction on top of the RESTful PredictionIO REST API interface.
 * <p>
 * All REST request methods come in both synchronous and asynchronous flavors via method overloading.
 * Synchronization methods are also provided via method overloading.
 * <p>
 * Multiple simultaneous asynchronous requests is made possible by the high performance backend provided by the <a href="https://github.com/AsyncHttpClient/async-http-client">Async Http Client</a>.
 *
 * @author The PredictionIO Team (<a href="http://prediction.io">http://prediction.io</a>)
 * @version 0.6.1
 * @since 0.1
 */
public class Client implements Closeable {
    // API base URL constant string
    private static final String defaultApiUrl = "http://localhost:8000";
    private static final String apiFormat = "json";
    private static final int defaultThreadLimit = 100;
    // HTTP status code
    private static final int HTTP_OK = 200;
    private static final int HTTP_CREATED = 201;
    private static final int HTTP_BAD_REQUEST = 400;
    private static final int HTTP_FORBIDDEN = 403;
    private static final int HTTP_NOT_FOUND = 404;
    //API Url
    private String apiUrl;
    // Appkey
    private String appkey;
    private AsyncHttpClient client;

    private JsonParser parser = new JsonParser();

    // internal field
    private String uid = null;

    /**
     * Instantiate a PredictionIO RESTful API client using default values for API URL and thread limit.
     * <p>
     * The default API URL is http://localhost:8000. The default thread limit is 100.
     *
     * @param appkey the app key that this client will use to communicate with the API
     */
    public Client(String appkey) {
        this(appkey, Client.defaultApiUrl, Client.defaultThreadLimit);
    }

    /**
     * Instantiate a PredictionIO RESTful API client using default values for API URL.
     * <p>
     * The default API URL is http://localhost:8000.
     *
     * @param appkey the app key that this client will use to communicate with the API
     * @param apiURL the URL of the PredictionIO API
     */
    public Client(String appkey, String apiURL) {
        this(appkey, apiURL, Client.defaultThreadLimit);
    }

    /**
     * Instantiate a PredictionIO RESTful API client.
     *
     * @param appkey the app key that this client will use to communicate with the API
     * @param apiURL the URL of the PredictionIO API
     * @param threadLimit maximum number of simultaneous threads (connections) to the API
     */
    public Client(String appkey, String apiURL, int threadLimit) {
        if (apiURL != null) {
            this.apiUrl = apiURL;
        } else {
            this.apiUrl = defaultApiUrl;
        }
        this.setAppkey(appkey);
        // Async HTTP client config
        AsyncHttpClientConfig config = (new AsyncHttpClientConfig.Builder())
                .setAllowPoolingConnection(true)
                .setAllowSslConnectionPool(true)
                .addRequestFilter(new ThrottleRequestFilter(threadLimit))
                .setMaximumConnectionsPerHost(threadLimit)
                .setRequestTimeoutInMs(10000)
                .setIOThreadMultiplier(threadLimit)
                .build();
        this.client = new AsyncHttpClient(new NettyAsyncHttpProvider(config), config);
    }

    /**
     * Close all connections associated with this client.
     * It is a good practice to always close the client after use.
     */
    @Override
    public void close() {
        this.client.close();
    }

    /**
     * Identify the user ID.
     * This identified user ID will be used for creating user-action-on-item request or getting item recommendation.
     *
     * @param uid ID of the User to be identified
     */
    public void identify(String uid) {
        this.uid = uid;
    }

    private AsyncHandler<APIResponse> getHandler() {
        return new AsyncHandler<APIResponse>() {
            private final Response.ResponseBuilder builder = new Response.ResponseBuilder();

            public void onThrowable(Throwable t) {
            }

            public STATE onBodyPartReceived(HttpResponseBodyPart content) throws Exception {
                builder.accumulate(content);
                return STATE.CONTINUE;
            }

            public STATE onStatusReceived(HttpResponseStatus status) throws Exception {
                builder.accumulate(status);
                return STATE.CONTINUE;
            }

            public STATE onHeadersReceived(HttpResponseHeaders headers) throws Exception {
                builder.accumulate(headers);
                return STATE.CONTINUE;
            }

            public APIResponse onCompleted() throws Exception {
                Response r = builder.build();
                return new APIResponse(r.getStatusCode(), r.getResponseBody());
            }
        };
    }

    private String[] jsonArrayAsStringArray(JsonArray a) throws ClassCastException {
        int l = a.size();
        String[] r = new String[l];
        for (int i = 0; i < l; i++) {
            JsonElement e = a.get(i);
            if (e.isJsonNull()) {
                r[i] = null;
            } else {
                r[i] = e.getAsString();
            }
        }
        return r;
    }

    private int[] jsonArrayAsIntArray(JsonArray a) throws ClassCastException {
        int l = a.size();
        int[] r = new int[l];
        for (int i = 0; i < l; i++) {
            r[i] = a.get(i).getAsInt();
        }
        return r;
    }

    private double[] jsonArrayAsDoubleArray(JsonArray a) throws ClassCastException {
        int l = a.size();
        double[] r = new double[l];
        for (int i = 0; i < l; i++) {
            r[i] = a.get(i).getAsDouble();
        }
        return r;
    }

    /**
     * Set the app key of this client.
     * <p>
     * All subsequent requests after this method will use the new app key.
     *
     * @param appkey the new app key to be used
     */
    public void setAppkey(String appkey) {
        // Set current API's appkey
        this.appkey = appkey;
    }

    /**
     * Set the API URL of this client.
     * <p>
     * All subsequent requests after this method will use the new API URL.
     *
     * @param apiUrl the new API URL to be used
     */
    public void setApiUrl(String apiUrl) {
        // Set current API's appkey
        this.apiUrl = apiUrl;
    }

    /**
     * Get status of the API.
     *
     * @throws ExecutionException indicates an error in the HTTP backend
     * @throws InterruptedException indicates an interruption during the HTTP operation
     * @throws IOException indicates an error from the API response
     */
    public String getStatus() throws ExecutionException, InterruptedException, IOException {
        return (new FutureAPIResponse(this.client.prepareGet(this.apiUrl).execute(this.getHandler()))).get().getMessage();
    }

    /**
     * Get a create user request builder that can be used to add additional user attributes.
     *
     * @param uid ID of the User to be created
     */
    public CreateUserRequestBuilder getCreateUserRequestBuilder(String uid) {
        return new CreateUserRequestBuilder(this.apiUrl, apiFormat, this.appkey, uid);
    }

    /**
     * Sends an asynchronous create user request to the API.
     *
     * @param builder an instance of {@link CreateUserRequestBuilder} that will be turned into a request
     */
    public FutureAPIResponse createUserAsFuture(CreateUserRequestBuilder builder) throws IOException {
        return new FutureAPIResponse(this.client.executeRequest(builder.build(), this.getHandler()));
    }

    /**
     * Sends a synchronous create user request to the API.
     *
     * @param uid ID of the User to be created
     *
     * @throws ExecutionException indicates an error in the HTTP backend
     * @throws InterruptedException indicates an interruption during the HTTP operation
     * @throws IOException indicates an error from the API response
     */
    public void createUser(String uid) throws ExecutionException, InterruptedException, IOException {
        this.createUser(this.createUserAsFuture(this.getCreateUserRequestBuilder(uid)));
    }

    /**
     * Sends a synchronous create user request to the API.
     *
     * @param builder an instance of {@link CreateUserRequestBuilder} that will be turned into a request
     *
     * @throws ExecutionException indicates an error in the HTTP backend
     * @throws InterruptedException indicates an interruption during the HTTP operation
     * @throws IOException indicates an error from the API response
     */
    public void createUser(CreateUserRequestBuilder builder) throws ExecutionException, InterruptedException, IOException {
        this.createUser(this.createUserAsFuture(builder));
    }

    /**
     * Synchronize a previously sent asynchronous create user request.
     *
     * @param response an instance of {@link FutureAPIResponse} returned from {@link Client#createUserAsFuture}
     *
     * @throws ExecutionException indicates an error in the HTTP backend
     * @throws InterruptedException indicates an interruption during the HTTP operation
     * @throws IOException indicates an error from the API response
     */
    public void createUser(FutureAPIResponse response) throws ExecutionException, InterruptedException, IOException {
        int status = response.get().getStatus();
        String message = response.get().getMessage();

        if (status != Client.HTTP_CREATED) {
            throw new IOException(message);
        }
    }

    /**
     * Sends an asynchronous get user request to the API.
     *
     * @param uid ID of the User to get
     */
    public FutureAPIResponse getUserAsFuture(String uid) throws IOException {
        Request request = (new RequestBuilder("GET")).setUrl(this.apiUrl + "/users/" + uid + "." + apiFormat).addQueryParameter("pio_appkey", this.appkey).build();
        return new FutureAPIResponse(this.client.executeRequest(request, this.getHandler()));
    }

    /**
     * Sends a synchronous get user request to the API.
     *
     * @param uid ID of the User to get
     *
     * @throws ExecutionException indicates an error in the HTTP backend
     * @throws InterruptedException indicates an interruption during the HTTP operation
     * @throws IOException indicates an error from the API response
     */
    public User getUser(String uid) throws ExecutionException, InterruptedException, IOException {
        return this.getUser(this.getUserAsFuture(uid));
    }

    /**
     * Synchronize a previously sent asynchronous get user request.
     *
     * @param response an instance of {@link FutureAPIResponse} returned from {@link Client#getUserAsFuture}
     *
     * @throws ExecutionException indicates an error in the HTTP backend
     * @throws InterruptedException indicates an interruption during the HTTP operation
     * @throws IOException indicates an error from the API response
     */
    public User getUser(FutureAPIResponse response) throws ExecutionException, InterruptedException, IOException {
        int status = response.get().getStatus();
        String message = response.get().getMessage();

        if (status == Client.HTTP_OK) {
            JsonObject messageAsJson = (JsonObject) parser.parse(message);
            String uid = messageAsJson.get("pio_uid").getAsString();
            User user = new User(uid);
            if (messageAsJson.getAsJsonArray("pio_latlng") != null) {
                double latlng[] = this.jsonArrayAsDoubleArray(messageAsJson.getAsJsonArray("pio_latlng"));
                user.latitude(new Double(latlng[0]));
                user.longitude(new Double(latlng[1]));
            }
            return user;
        } else {
            throw new IOException(message);
        }
    }

    /**
     * Sends an asynchronous delete user request to the API.
     *
     * @param uid ID of the User to be deleted
     */
    public FutureAPIResponse deleteUserAsFuture(String uid) throws IOException {
        RequestBuilder builder = new RequestBuilder("DELETE");
        builder.setUrl(this.apiUrl + "/users/" + uid + "." + apiFormat);
        builder.addQueryParameter("pio_appkey", this.appkey);
        return new FutureAPIResponse(this.client.executeRequest(builder.build(), this.getHandler()));
    }

    /**
     * Sends a synchronous delete user request to the API.
     *
     * @param uid ID of the User to be deleted
     *
     * @throws ExecutionException indicates an error in the HTTP backend
     * @throws InterruptedException indicates an interruption during the HTTP operation
     * @throws IOException indicates an error from the API response
     */
    public void deleteUser(String uid) throws ExecutionException, InterruptedException, IOException {
        this.deleteUser(this.deleteUserAsFuture(uid));
    }

    /**
     * Synchronize a previously sent asynchronous delete user request.
     *
     * @param response an instance of {@link FutureAPIResponse} returned from {@link Client#deleteUserAsFuture}
     *
     * @throws ExecutionException indicates an error in the HTTP backend
     * @throws InterruptedException indicates an interruption during the HTTP operation
     * @throws IOException indicates an error from the API response
     */
    public void deleteUser(FutureAPIResponse response) throws ExecutionException, InterruptedException, IOException {
        int status = response.get().getStatus();
        String message = response.get().getMessage();

        if (status != Client.HTTP_OK) {
            throw new IOException(message);
        }
    }

    /**
     * Get a create item request builder that can be used to add additional item attributes.
     *
     * @param iid ID of the Item to be created
     * @param itypes array of types of the Item
     */
    public CreateItemRequestBuilder getCreateItemRequestBuilder(String iid, String[] itypes) {
        return new CreateItemRequestBuilder(this.apiUrl, apiFormat, this.appkey, iid, itypes);
    }

    /**
     * Sends an asynchronous create item request to the API.
     *
     * @param builder an instance of {@link CreateItemRequestBuilder} that will be turned into a request
     */
    public FutureAPIResponse createItemAsFuture(CreateItemRequestBuilder builder) throws IOException {
        return new FutureAPIResponse(this.client.executeRequest(builder.build(), this.getHandler()));
    }

    /**
     * Sends a synchronous create item request to the API.
     *
     * @param iid ID of the Item to be created
     * @param itypes array of types of the Item
     *
     * @throws ExecutionException indicates an error in the HTTP backend
     * @throws InterruptedException indicates an interruption during the HTTP operation
     * @throws IOException indicates an error from the API response
     */
    public void createItem(String iid, String[] itypes) throws ExecutionException, InterruptedException, IOException {
        this.createItem(this.createItemAsFuture(this.getCreateItemRequestBuilder(iid, itypes)));
    }

    /**
     * Sends a synchronous create item request to the API.
     *
     * @param builder an instance of {@link CreateItemRequestBuilder} that will be turned into a request
     *
     * @throws ExecutionException indicates an error in the HTTP backend
     * @throws InterruptedException indicates an interruption during the HTTP operation
     * @throws IOException indicates an error from the API response
     */
    public void createItem(CreateItemRequestBuilder builder) throws ExecutionException, InterruptedException, IOException {
        this.createItem(this.createItemAsFuture(builder));
    }

    /**
     * Synchronize a previously sent asynchronous create item request.
     *
     * @param response an instance of {@link FutureAPIResponse} returned from {@link Client#createItemAsFuture}
     *
     * @throws ExecutionException indicates an error in the HTTP backend
     * @throws InterruptedException indicates an interruption during the HTTP operation
     * @throws IOException indicates an error from the API response
     */
    public void createItem(FutureAPIResponse response) throws ExecutionException, InterruptedException, IOException {
        int status = response.get().getStatus();
        String message = response.get().getMessage();

        if (status != Client.HTTP_CREATED) {
            throw new IOException(message);
        }
    }

    /**
     * Sends an asynchronous get item request to the API.
     *
     * @param iid ID of the Item to get
     */
    public FutureAPIResponse getItemAsFuture(String iid) throws IOException {
        Request request = (new RequestBuilder("GET")).setUrl(this.apiUrl + "/items/" + iid + "." + apiFormat).addQueryParameter("pio_appkey", this.appkey).build();
        return new FutureAPIResponse(this.client.executeRequest(request, this.getHandler()));
    }

    /**
     * Sends a synchronous get item request to the API.
     *
     * @param iid ID of the Item to get
     *
     * @throws ExecutionException indicates an error in the HTTP backend
     * @throws InterruptedException indicates an interruption during the HTTP operation
     * @throws IOException indicates an error from the API response
     */
    public Item getItem(String iid) throws ExecutionException, InterruptedException, IOException {
        return this.getItem(this.getItemAsFuture(iid));
    }

    /**
     * Synchronize a previously sent asynchronous get item request.
     *
     * @param response an instance of {@link FutureAPIResponse} returned from {@link Client#getItemAsFuture}
     *
     * @throws ExecutionException indicates an error in the HTTP backend
     * @throws InterruptedException indicates an interruption during the HTTP operation
     * @throws IOException indicates an error from the API response
     */
    public Item getItem(FutureAPIResponse response) throws ExecutionException, InterruptedException, IOException {
        int status = response.get().getStatus();
        String message = response.get().getMessage();

        if (status == Client.HTTP_OK) {
            JsonObject messageAsJson = (JsonObject) parser.parse(message);
            String iid = messageAsJson.get("pio_iid").getAsString();
            String[] itypes = this.jsonArrayAsStringArray(messageAsJson.getAsJsonArray("pio_itypes"));
            Item item = new Item(iid, itypes);
            if (messageAsJson.get("pio_startT") != null) {
                item.startT(new Date(messageAsJson.get("pio_startT").getAsLong()));
            }
            if (messageAsJson.get("pio_endT") != null) {
                item.endT(new Date(messageAsJson.get("pio_endT").getAsLong()));
            }
            if (messageAsJson.getAsJsonArray("pio_latlng") != null) {
                double latlng[] = this.jsonArrayAsDoubleArray(messageAsJson.getAsJsonArray("pio_latlng"));
                item.latitude(new Double(latlng[0]));
                item.longitude(new Double(latlng[1]));
            }
            return item;
        } else {
            throw new IOException(message);
        }
    }

    /**
     * Sends an asynchronous delete item request to the API.
     *
     * @param iid ID of the Item to be deleted
     */
    public FutureAPIResponse deleteItemAsFuture(String iid) throws IOException {
        RequestBuilder builder = new RequestBuilder("DELETE");
        builder.setUrl(this.apiUrl + "/items/" + iid + "." + apiFormat);
        builder.addQueryParameter("pio_appkey", this.appkey);
        return new FutureAPIResponse(this.client.executeRequest(builder.build(), this.getHandler()));
    }

    /**
     * Sends a synchronous delete item request to the API.
     *
     * @param iid ID of the Item to be deleted
     *
     * @throws ExecutionException indicates an error in the HTTP backend
     * @throws InterruptedException indicates an interruption during the HTTP operation
     * @throws IOException indicates an error from the API response
     */
    public void deleteItem(String iid) throws ExecutionException, InterruptedException, IOException {
        this.deleteItem(this.deleteItemAsFuture(iid));
    }

    /**
     * Synchronize a previously sent asynchronous delete item request.
     *
     * @param response an instance of {@link FutureAPIResponse} returned from {@link Client#deleteItemAsFuture}
     *
     * @throws ExecutionException indicates an error in the HTTP backend
     * @throws InterruptedException indicates an interruption during the HTTP operation
     * @throws IOException indicates an error from the API response
     */
    public void deleteItem(FutureAPIResponse response) throws ExecutionException, InterruptedException, IOException {
        int status = response.get().getStatus();
        String message = response.get().getMessage();

        if (status != Client.HTTP_OK) {
            throw new IOException(message);
        }
    }

    /**
     * Get a get top-n recommendations request builder that can be used to add additional request parameters.
     *
     * Using this method overrides the user ID set by {@link Client#identify}.
     *
     * @param engine engine name
     * @param uid ID of the User whose recommendations will be gotten
     * @param n number of top recommendations to get
     */
    public ItemRecGetTopNRequestBuilder getItemRecGetTopNRequestBuilder(String engine, String uid, int n) {
        return new ItemRecGetTopNRequestBuilder(this.apiUrl, apiFormat, this.appkey, engine, uid, n);
    }

    /**
     * Get a get top-n recommendations request builder that can be used to add additional request parameters.
     * Identified user ID will be used. See {@link Client#identify}.
     *
     * @param engine engine name
     * @param n number of top recommendations to get
     *
     * @throws UnidentifiedUserException indicates an unidentified user ID error
     */
    public ItemRecGetTopNRequestBuilder getItemRecGetTopNRequestBuilder(String engine, int n) throws UnidentifiedUserException {
        if (this.uid == null) {
            throw new UnidentifiedUserException("User ID has not been identified. Please call identify(uid) first.");
        }
        return new ItemRecGetTopNRequestBuilder(this.apiUrl, apiFormat, this.appkey, engine, this.uid, n);
    }

    /**
     * Get a get top-n recommendations request builder that can be used to add additional request parameters.
     *
     * Using this method overrides the user ID set by {@link Client#identify}.
     *
     * @param engine engine name
     * @param uid ID of the User whose recommendations will be gotten
     * @param n number of top recommendations to get
     * @param attributes array of item attribute names to be returned with the result
     */
    public ItemRecGetTopNRequestBuilder getItemRecGetTopNRequestBuilder(String engine, String uid, int n, String[] attributes) {
        return (new ItemRecGetTopNRequestBuilder(this.apiUrl, apiFormat, this.appkey, engine, uid, n)).attributes(attributes);
    }

    /**
     * Get a get top-n recommendations request builder that can be used to add additional request parameters.
     * Identified user ID will be used. See {@link Client#identify}.
     *
     * @param engine engine name
     * @param n number of top recommendations to get
     * @param attributes array of item attribute names to be returned with the result
     *
     * @throws UnidentifiedUserException indicates an unidentified user ID error
     */
    public ItemRecGetTopNRequestBuilder getItemRecGetTopNRequestBuilder(String engine, int n, String[] attributes) throws UnidentifiedUserException {
        if (this.uid == null) {
            throw new UnidentifiedUserException("User ID has not been identified. Please call identify(uid) first.");
        }
        return (new ItemRecGetTopNRequestBuilder(this.apiUrl, apiFormat, this.appkey, engine, this.uid, n)).attributes(attributes);
    }

    /**
     * Sends an asynchronous get recommendations request to the API.
     *
     * @param builder an instance of {@link ItemRecGetTopNRequestBuilder} that will be turned into a request
     */
    public FutureAPIResponse getItemRecTopNAsFuture(ItemRecGetTopNRequestBuilder builder) throws IOException {
        return new FutureAPIResponse(this.client.executeRequest(builder.build(), this.getHandler()));
    }

    /**
     * Sends a synchronous get recommendations request to the API.
     *
     * Using this method overrides the user ID set by {@link Client#identify}.
     *
     * @param engine engine name
     * @param uid ID of the User whose recommendations will be gotten
     * @param n number of top recommendations to get
     *
     * @throws ExecutionException indicates an error in the HTTP backend
     * @throws InterruptedException indicates an interruption during the HTTP operation
     * @throws IOException indicates an error from the API response
     */
    public String[] getItemRecTopN(String engine, String uid, int n) throws ExecutionException, InterruptedException, IOException {
        return this.getItemRecTopN(this.getItemRecTopNAsFuture(this.getItemRecGetTopNRequestBuilder(engine, uid, n)));
    }

    /**
     * Sends a synchronous get recommendations request to the API.
     * Identified user ID will be used. See {@link Client#identify}.
     *
     * @param engine engine name
     * @param n number of top recommendations to get
     *
     * @throws UnidentifiedUserException indicates an unidentified user ID error
     * @throws ExecutionException indicates an error in the HTTP backend
     * @throws InterruptedException indicates an interruption during the HTTP operation
     * @throws IOException indicates an error from the API response
     */
    public String[] getItemRecTopN(String engine, int n) throws UnidentifiedUserException, ExecutionException, InterruptedException, IOException {
        return this.getItemRecTopN(this.getItemRecTopNAsFuture(this.getItemRecGetTopNRequestBuilder(engine, n)));
    }

    /**
     * Sends a synchronous get recommendations request to the API.
     *
     * @param builder an instance of {@link ItemRecGetTopNRequestBuilder} that will be turned into a request
     *
     * @throws ExecutionException indicates an error in the HTTP backend
     * @throws InterruptedException indicates an interruption during the HTTP operation
     * @throws IOException indicates an error from the API response
     */
    public String[] getItemRecTopN(ItemRecGetTopNRequestBuilder builder) throws ExecutionException, InterruptedException, IOException {
        return this.getItemRecTopN(this.getItemRecTopNAsFuture(builder));
    }

    /**
     * Synchronize a previously sent asynchronous get recommendations request.
     *
     * @param response an instance of {@link FutureAPIResponse} returned from {@link Client#getItemRecTopNAsFuture}
     *
     * @throws ExecutionException indicates an error in the HTTP backend
     * @throws InterruptedException indicates an interruption during the HTTP operation
     * @throws IOException indicates an error from the API response
     */
    public String[] getItemRecTopN(FutureAPIResponse response) throws ExecutionException, InterruptedException, IOException {
        // Do not use getStatus/getMessage directly as they do not pass exceptions
        int status = response.get().getStatus();
        String message = response.get().getMessage();

        if (status == Client.HTTP_OK) {
            JsonObject messageAsJson = (JsonObject) parser.parse(message);
            JsonArray iidsAsJson = messageAsJson.getAsJsonArray("pio_iids");
            return this.jsonArrayAsStringArray(iidsAsJson);
        } else {
            throw new IOException(message);
        }
    }

    /**
     * Sends a synchronous get recommendations request to the API.
     *
     * Using this method overrides the user ID set by {@link Client#identify}.
     *
     * @param engine engine name
     * @param uid ID of the User whose recommendations will be gotten
     * @param n number of top recommendations to get
     * @param attributes array of item attribute names to be returned with the result
     *
     * @throws ExecutionException indicates an error in the HTTP backend
     * @throws InterruptedException indicates an interruption during the HTTP operation
     * @throws IOException indicates an error from the API response
     */
    public Map<String, String[]> getItemRecTopNWithAttributes(String engine, String uid, int n, String[] attributes) throws ExecutionException, InterruptedException, IOException {
        return this.getItemRecTopNWithAttributes(this.getItemRecTopNAsFuture(this.getItemRecGetTopNRequestBuilder(engine, uid, n, attributes)));
    }

    /**
     * Sends a synchronous get recommendations request to the API.
     * Identified user ID will be used. See {@link Client#identify}.
     *
     * @param engine engine name
     * @param n number of top recommendations to get
     * @param attributes array of item attribute names to be returned with the result
     *
     * @throws UnidentifiedUserException indicates an unidentified user ID error
     * @throws ExecutionException indicates an error in the HTTP backend
     * @throws InterruptedException indicates an interruption during the HTTP operation
     * @throws IOException indicates an error from the API response
     */
    public Map<String, String[]> getItemRecTopNWithAttributes(String engine, int n, String[] attributes) throws UnidentifiedUserException, ExecutionException, InterruptedException, IOException {
        return this.getItemRecTopNWithAttributes(this.getItemRecTopNAsFuture(this.getItemRecGetTopNRequestBuilder(engine, n, attributes)));
    }

    /**
     * Sends a synchronous get recommendations request to the API.
     *
     * @param builder an instance of {@link ItemRecGetTopNRequestBuilder} that will be turned into a request
     *
     * @throws ExecutionException indicates an error in the HTTP backend
     * @throws InterruptedException indicates an interruption during the HTTP operation
     * @throws IOException indicates an error from the API response
     */
    public Map<String, String[]> getItemRecTopNWithAttributes(ItemRecGetTopNRequestBuilder builder) throws ExecutionException, InterruptedException, IOException {
        return this.getItemRecTopNWithAttributes(this.getItemRecTopNAsFuture(builder));
    }

    /**
     * Synchronize a previously sent asynchronous get recommendations request.
     *
     * @param response an instance of {@link FutureAPIResponse} returned from {@link Client#getItemRecTopNAsFuture}
     *
     * @throws ExecutionException indicates an error in the HTTP backend
     * @throws InterruptedException indicates an interruption during the HTTP operation
     * @throws IOException indicates an error from the API response
     */
    public Map<String, String[]> getItemRecTopNWithAttributes(FutureAPIResponse response) throws ExecutionException, InterruptedException, IOException {
        // Do not use getStatus/getMessage directly as they do not pass exceptions
        int status = response.get().getStatus();
        String message = response.get().getMessage();

        if (status == Client.HTTP_OK) {
            Map<String, String[]> results = new HashMap<String, String[]>();
            JsonObject messageAsJson = (JsonObject) parser.parse(message);
            for (Map.Entry<String, JsonElement> member : messageAsJson.entrySet()) {
                results.put(member.getKey(), this.jsonArrayAsStringArray(member.getValue().getAsJsonArray()));
            }
            return results;
        } else {
            throw new IOException(message);
        }
    }

    /**
     * Get a get top-n similar items request builder that can be used to add additional request parameters.
     *
     * @param engine engine name
     * @param iid ID of the Item
     * @param n number of top similar items to get
     */
    public ItemSimGetTopNRequestBuilder getItemSimGetTopNRequestBuilder(String engine, String iid, int n) {
        return new ItemSimGetTopNRequestBuilder(this.apiUrl, apiFormat, this.appkey, engine, iid, n);
    }

    /**
     * Get a get top-n similar items request builder that can be used to add additional request parameters.
     *
     * @param engine engine name
     * @param iid ID of the Item
     * @param n number of top similar items to get
     * @param attributes array of item attribute names to be returned with the result
     */
    public ItemSimGetTopNRequestBuilder getItemSimGetTopNRequestBuilder(String engine, String iid, int n, String[] attributes) {
        return new ItemSimGetTopNRequestBuilder(this.apiUrl, apiFormat, this.appkey, engine, iid, n).attributes(attributes);
    }

    /**
     * Sends an asynchronous get similar items request to the API.
     *
     * @param builder an instance of {@link ItemSimGetTopNRequestBuilder} that will be turned into a request
     */
    public FutureAPIResponse getItemSimTopNAsFuture(ItemSimGetTopNRequestBuilder builder) throws IOException {
        return new FutureAPIResponse(this.client.executeRequest(builder.build(), this.getHandler()));
    }

    /**
     * Sends a synchronous get similar items request to the API.
     *
     * @param engine engine name
     * @param iid ID of the Item
     * @param n number of top recommendations to get
     *
     * @throws ExecutionException indicates an error in the HTTP backend
     * @throws InterruptedException indicates an interruption during the HTTP operation
     * @throws IOException indicates an error from the API response
     */
    public String[] getItemSimTopN(String engine, String iid, int n) throws ExecutionException, InterruptedException, IOException {
        return this.getItemSimTopN(this.getItemSimTopNAsFuture(this.getItemSimGetTopNRequestBuilder(engine, iid, n)));
    }

    /**
     * Sends a synchronous get similar items request to the API.
     *
     * @param builder an instance of {@link ItemSimGetTopNRequestBuilder} that will be turned into a request
     *
     * @throws ExecutionException indicates an error in the HTTP backend
     * @throws InterruptedException indicates an interruption during the HTTP operation
     * @throws IOException indicates an error from the API response
     */
    public String[] getItemSimTopN(ItemSimGetTopNRequestBuilder builder) throws ExecutionException, InterruptedException, IOException {
        return this.getItemSimTopN(this.getItemSimTopNAsFuture(builder));
    }

    /**
     * Synchronize a previously sent asynchronous get similar items request.
     *
     * @param response an instance of {@link FutureAPIResponse} returned from {@link Client#getItemSimTopNAsFuture}
     *
     * @throws ExecutionException indicates an error in the HTTP backend
     * @throws InterruptedException indicates an interruption during the HTTP operation
     * @throws IOException indicates an error from the API response
     */
    public String[] getItemSimTopN(FutureAPIResponse response) throws ExecutionException, InterruptedException, IOException {
        // Do not use getStatus/getMessage directly as they do not pass exceptions
        int status = response.get().getStatus();
        String message = response.get().getMessage();

        if (status == Client.HTTP_OK) {
            JsonObject messageAsJson = (JsonObject) parser.parse(message);
            JsonArray iidsAsJson = messageAsJson.getAsJsonArray("pio_iids");
            return this.jsonArrayAsStringArray(iidsAsJson);
        } else {
            throw new IOException(message);
        }
    }

    /**
     * Sends a synchronous get similar items request to the API.
     *
     * @param engine engine name
     * @param iid ID of the Item
     * @param n number of top recommendations to get
     * @param attributes array of item attribute names to be returned with the result
     *
     * @throws ExecutionException indicates an error in the HTTP backend
     * @throws InterruptedException indicates an interruption during the HTTP operation
     * @throws IOException indicates an error from the API response
     */
    public Map<String, String[]> getItemSimTopNWithAttributes(String engine, String iid, int n, String[] attributes) throws ExecutionException, InterruptedException, IOException {
        return this.getItemSimTopNWithAttributes(this.getItemSimTopNAsFuture(this.getItemSimGetTopNRequestBuilder(engine, iid, n, attributes)));
    }

    /**
     * Sends a synchronous get similar items request to the API.
     *
     * @param builder an instance of {@link ItemSimGetTopNRequestBuilder} that will be turned into a request
     *
     * @throws ExecutionException indicates an error in the HTTP backend
     * @throws InterruptedException indicates an interruption during the HTTP operation
     * @throws IOException indicates an error from the API response
     */
    public Map<String, String[]> getItemSimTopNWithAttributes(ItemSimGetTopNRequestBuilder builder) throws ExecutionException, InterruptedException, IOException {
        return this.getItemSimTopNWithAttributes(this.getItemSimTopNAsFuture(builder));
    }

    /**
     * Synchronize a previously sent asynchronous get similar items request.
     *
     * @param response an instance of {@link FutureAPIResponse} returned from {@link Client#getItemSimTopNAsFuture}
     *
     * @throws ExecutionException indicates an error in the HTTP backend
     * @throws InterruptedException indicates an interruption during the HTTP operation
     * @throws IOException indicates an error from the API response
     */
    public Map<String, String[]> getItemSimTopNWithAttributes(FutureAPIResponse response) throws ExecutionException, InterruptedException, IOException {
        // Do not use getStatus/getMessage directly as they do not pass exceptions
        int status = response.get().getStatus();
        String message = response.get().getMessage();

        if (status == Client.HTTP_OK) {
            Map<String, String[]> results = new HashMap<String, String[]>();
            JsonObject messageAsJson = (JsonObject) parser.parse(message);
            for (Map.Entry<String, JsonElement> member : messageAsJson.entrySet()) {
                results.put(member.getKey(), this.jsonArrayAsStringArray(member.getValue().getAsJsonArray()));
            }
            return results;
        } else {
            throw new IOException(message);
        }
    }

    /**
     * Get a user-action-on-item request builder that can be used to add additional request parameters.
     *
     * Using this method overrides the user ID set by {@link Client#identify}.
     *
     * @param uid ID of the User of this action
     * @param action action name
     * @param iid ID of the Item of this action
     */
    public UserActionItemRequestBuilder getUserActionItemRequestBuilder(String uid, String action, String iid) {
        UserActionItemRequestBuilder builder = new UserActionItemRequestBuilder(this.apiUrl, apiFormat, this.appkey, action, uid, iid);
        return builder;
    }

    /**
     * Get a user-action-on-item request builder that can be used to add additional request parameters.
     * Identified user ID will be used. See {@link Client#identify}.
     *
     * @param action action name
     * @param iid ID of the Item of this action
     *
     * @throws UnidentifiedUserException indicates an unidentified user ID error
     */
    public UserActionItemRequestBuilder getUserActionItemRequestBuilder(String action, String iid) throws UnidentifiedUserException {
        if (this.uid == null) {
            throw new UnidentifiedUserException("User ID has not been identified. Please call identify(uid) first.");
        }
        UserActionItemRequestBuilder builder = new UserActionItemRequestBuilder(this.apiUrl, apiFormat, this.appkey, action, this.uid, iid);
        return builder;
    }

    /**
     * Sends an asynchronous user-action-on-item request to the API.
     *
     * Using this method overrides the user ID set by {@link Client#identify}.
     *
     * @param uid ID of the User of this action
     * @param action action name
     * @param iid ID of the Item of this action
     */
    public FutureAPIResponse userActionItemAsFuture(String uid, String action, String iid) throws IOException {
        return this.userActionItemAsFuture(this.getUserActionItemRequestBuilder(uid, action, iid));
    }

    /**
     * Sends an asynchronous user-action-on-item request to the API.
     * Identified user ID will be used. See {@link Client#identify}.
     *
     * @param action action name
     * @param iid ID of the Item of this action
     *
     * @throws UnidentifiedUserException indicates an unidentified user ID error
     */
    public FutureAPIResponse userActionItemAsFuture(String action, String iid) throws UnidentifiedUserException, IOException {
        return this.userActionItemAsFuture(this.getUserActionItemRequestBuilder(action, iid));
    }

    /**
     * Sends an asynchronous user-action-on-item request to the API.
     *
     * @param builder an instance of {@link UserActionItemRequestBuilder} that will be turned into a request
     */
    public FutureAPIResponse userActionItemAsFuture(UserActionItemRequestBuilder builder) throws IOException {
        return new FutureAPIResponse(this.client.executeRequest(builder.build(), this.getHandler()));
    }

    /**
     * Sends a synchronous user-action-on-item request to the API.
     *
     * Using this method overrides the user ID set by {@link Client#identify}.
     *
     * @param uid ID of the User of this action
     * @param action action name
     * @param iid ID of the Item of this action
     *
     * @throws ExecutionException indicates an error in the HTTP backend
     * @throws InterruptedException indicates an interruption during the HTTP operation
     * @throws IOException indicates an error from the API response
     */
    public void userActionItem(String uid, String action, String iid) throws ExecutionException, InterruptedException, IOException {
        this.userActionItem(this.userActionItemAsFuture(this.getUserActionItemRequestBuilder(uid, action, iid)));
    }

    /**
     * Sends a synchronous user-action-on-item request to the API.
     * Identified user ID will be used. See {@link Client#identify}.
     *
     * @param action action name
     * @param iid ID of the Item of this action
     *
     * @throws UnidentifiedUserException indicates an unidentified user ID error
     * @throws ExecutionException indicates an error in the HTTP backend
     * @throws InterruptedException indicates an interruption during the HTTP operation
     * @throws IOException indicates an error from the API response
     */
    public void userActionItem(String action, String iid) throws UnidentifiedUserException, ExecutionException, InterruptedException, IOException {
        this.userActionItem(this.userActionItemAsFuture(this.getUserActionItemRequestBuilder(action, iid)));
    }

    /**
     * Sends a synchronous user-rate-item action request to the API.
     *
     * @param builder an instance of {@link UserActionItemRequestBuilder} that will be turned into a request
     *
     * @throws ExecutionException indicates an error in the HTTP backend
     * @throws InterruptedException indicates an interruption during the HTTP operation
     * @throws IOException indicates an error from the API response
     */
    public void userActionItem(UserActionItemRequestBuilder builder) throws ExecutionException, InterruptedException, IOException {
        this.userActionItem(this.userActionItemAsFuture(builder));
    }

    /**
     * Synchronize a previously sent asynchronous user-action-on-item request.
     *
     * @param response an instance of {@link FutureAPIResponse} returned from {@link Client#userRateItemAsFuture}
     *
     * @throws ExecutionException indicates an error in the HTTP backend
     * @throws InterruptedException indicates an interruption during the HTTP operation
     * @throws IOException indicates an error from the API response
     */
    public void userActionItem(FutureAPIResponse response) throws ExecutionException, InterruptedException, IOException {
        int status = response.get().getStatus();
        String message = response.get().getMessage();

        if (status != Client.HTTP_CREATED) {
            throw new IOException(message);
        }
    }
}
