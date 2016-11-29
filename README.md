# nFusion
 
It is not a 90s boy band, but rather a unified approach to endpoint IO over HTTP.

Tiny (around 1 kLoC) library that implements a generic way of communicating with a HTTP REST backend from different concurrent contexts (activites and fragments mainly).

## Features
- Wrap's endpoints and provides automatic caching, presenting implementers with a simple to use subscription based interface.
- Catches exceptions and combines all error checkning into a simple structure EndPointError, to simply error checking in the app.
- Features lazy loading and initialization. Endpoints and their caches gets instantiated and initialized (loaded) asynchronously when your app subscribes to them for the first time. Data requests automatically gets queued until endpoint initialization is done.
- Eventbus functionality. If one subscriber requests fresh data from an endpoint, other subscribers gets automatically notified.
- Automatic disk caching with auto generation of cache id based on endpoint parameter values. Subscribers always gets cached data if its available.
- Plugable authentication. Auth is implementing trough the IAuthentication interface and can be specified per endpoint.
- Plugable caching trough the ICache interface, a basic implementation is provided in the form of the SimpleDiskCache class.
- Makes the opposite sex more attracted to you, by expelling a potent love pheromone which is the result of years of research on ants & moose.

## Design
nFusion is basically a worker thread sequentially executing endpoint requests from a queue, while calling a couple of interfaces which implements an abstract concept of doing the IO, in the process. The actual data loading / parsing is left to the implementer. It is however designed to be used in conjection with Google GSON (for parsing) and depends on it (although only for the parsing exceptions, due to error unification feature).

### Goals

- Low cohesion: All the communication between objects are done trough interfaces where each implementer act as a black box.
- Low coupling: Caching and authentication is derived from interfaces and can even be implemented in the client app.
- Provide a single interface to client (eg activity, fragment) to implement inorder to request and receive data.
- Simplify and wrap the complexity of communicating with a HTTP endpoint, from a UI centric perspective.
- Hide complexity of doing asynchronous IO and lazy loading, from the client app.
- Cut down on the usual arrow code spaghetti present in each view and trade it for one giant ball in the library. Aka the nested listeners of doom antipattern (tm).
- Make it easy to implement long complicated auth chains like OAuth where you trade in different tokens and in the worst case scenario have to collect data from the user (usually by presenting some form of login view).
- Utilize recent advances made in the field of ant <-> moose interpersonal relationship research.

### Limitations
- The library current only spawns one worker thread and thus cannot perform simultanious requests. It is a future feature because concurrent code is complicated and I mostly spend one saturday evening on this :D.

## Installation
### Install the library
1. Clone project
2. Open with android studio
3. Run gradle task installArchives in the app modules build file (build.gradle in the app subdir, you can right click it to automatically run it and generate a runtime target for future use).
4. Library is now uploaded to your local maven repository

### Add the library to your project
After following the procedure above, follow these steps to use the library in your app project:

- Make sure your app's global or module build file include the maven local repository. This looks like this:

```ruby
repositories {
    mavenCentral()
    mavenLocal()
}
```

- If mavenLocal() is not in there, you need to add it.

- Include this in your dependencies in the module where you wish to use the library:

```ruby
compile('dk.nodes.nfusion:v1:+')
```

## Dependencies
- OkHTTP
- OkHTTP:logging-interceptor
- Google GSON


## Usage
### Implement endpoint
The example code below wraps an unprotected endpoint which returns a Person model object

```java
public class PersonEndPoint extends EndPoint<Person> {
    // Factory function called by nFusion to create a cache for this object type
    // Create your own cache deriving from ICache and use it here if you'd like.
    @Override
    public ICache<Person> obtainCacheInstance(Context context, Class model_cls) {
        return new SimpleDiskCache<>(context, model_cls);
    }

    // Factory function for obtaining authentication instance. Implement your own
    // auth scheme in a class implementing IAuthentication and use here.
    // returning null means no auth.
    @Override
    public IAuthentication obtainAuthenticationInstance(Context context) {
        return null;
    }

    // This is the concrete implementation of the abstract process of fetching
    // data from the endpoint. This is where you implement the actual http request
    // to the end point. All exceptions gets trapped by the abstract base implementation 
    // (see EndPoint.java), but you can augment the error handling as shown below.
    // The implementation utilizes the shared OkHTTP client owned by the nFusion class
    // this makes it possible to inject token headers in a generic way for auth etc.
    @Override
    public Person call(EndPointRequest request) throws Exception {
        nFusion epm = nFusion.instance();
        String person_name = request.getString("name");
        Request http_request = epm.buildGetRequest(ApiConfig.API_URL + "people/" + name);
        Response response = epm.executeRequest(http_request);
        int error_code = response.code();
        if(response.isSuccessful())
        {
            String content = new String(response.body().bytes());
            Gson gson = new GsonBuilder().create();
            Person person = gson.fromJson(content, Person.class);
            return person;
        }
        else // error happened. This is mostly for errors not producing exceptions
        {
            EndPointError error = new EndPointError(EndPointError.HTTP_ERROR, error_code, response, http_request);
            setError(error);
        }
        return null;
    }
}
```

### Use endpoint implementation
To use the endpoint we made above, we must implement a class derived from Subscription and as a minimum
implements the onUpdate() function. This is makes sense to have as a private class inside your activity or fragment.

Example:

```java
private class PersonSubscription extends Subscription<Person>
{
    public PersonSubscription(Class cls, Class model_cls) {
        super(cls, model_cls);
    }

    @Override
    public void onUpdate(Person person) {
        Log.d(TAG, "onUpdate received person: " + person.toString());
        // update your UI here
    }
}
```

Now you need to setup an instance of this subscription class, here is an example based on a complete activity for reference:
```java
public class MainActivity extends Activity {
    public static final String TAG = MainActivity.class.getSimpleName();
    PersonSubscription personSub;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // initialize nFusion, in a real app you'd probably call this in your Application's onCreate method
        // you need to pass this a context that will live for the duration of the app
        nFusion.instance().init(getApplicationContext());

        // Instantiate and configure the subscription private class we've implemented below
        personSub = new PersonSubscription(PersonEndpoint.class, Person.class);
        // we call subscribe meaning that from now on, onUpdate in our PersonSubscription gets called
        // with fresh data, until we call unSubscribe().
        personSub.subscribe();

        // Instantiate and configure a request to the endpoint. The request object derives from JSONObject
        // and can be treated the same way. This is intended as a generic way of passing parameters to the implementation
        // of the endpoint (see code sample above in this README).
        // nFusion also generates a unique hash of this request based on the parameters, which is used by the caching mechanism.
        EndPointRequest person_req = new EndPointRequest();
        person_req.put("name", "dollarklaus");

        // we submit the request. Sooner or later onUpdate or onError in PersonSubscription gets called, depending on the outcome.
        // if there is cached data present for this request, onUpdate gets called immediately. If said data is either stale or
        // force refresh is set, the request is immediately queued for refreshing and nFusion will execute the http call trough
        // the endpoint and return with an update or error.
        personSub.request(person_req);


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        personSub.unSubscribe();
    }

    private class PersonSubscription extends Subscription<Person> {
        public PersonSubscription(Class cls, Class model_cls) {
            super(cls, model_cls);
        }

        @Override
        public void onUpdate(Person person) {
            Log.d(TAG, "onUpdate received person: " + person.toString());
            // update your UI here
        }

        @Override
        public void onError(EndPointError error) {
            // call base implementation to dump a bit of info to the log, not required
            super.onError(error);
            switch (error.code) {
                case EndPointError.UNKNOWN_HOST_ERROR:
                    // inform user there is no data connection
                    break;
                case EndPointError.HTTP_ERROR:
                    // inform user a transport error happened, error.httpCode has the http error code
                    break;
                case EndPointError.JSON_ERROR:
                    // inform user that data from backend could not be parsed
                    break;
                case EndPointError.REQUEST_PARAM_ERROR:
                    // this happens if endpoint implementation miss parameters
                    break;
                case EndPointError.FAILED_AUTH_ERROR:
                    // this happens if its not possible to obtain auth in any way on a protected endpoint
                    break;
                default:
                    // default handler, some really obscure shit happened or a bug
                    break;
            }
        }
    }
}
```
See the nfusion-sample-oauth in the ournodes repository for working sample code.
See also DFDS project in ournodes repository for a lot of working code.

 

