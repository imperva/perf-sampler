# Production grade JAVA performance sampler
A light weight JAVA performance tool. Its sampling technique allows 24x7 performance monitoring in production with negligible and predicted overhead.

### A bit on sampling theory
This sampler does not measure the amount of executions and duration of each and every method. That would dramatically impact the resposiveness of your application. 

To allow 24x7 performance monitoring in production environment, a sampler is a much better fit. Its overhead is much lower, predictable and controlable.
It periodically takes a snapshot of all JAVA threads using `Thread.getAllStackTraces()` call. Let's say sampling frequency is once every 50ms. Then, the current active method and line of code is counted as responsible for the entire period (50ms). This is done for each thread and aggregated over time.

Of course, not all the accounted time was actually spent that arbirary method. However, if you sample your thread for 50 seconds (1000 samples once every 50ms) and a specific method was reported as current in 200 different samples, then it's safe to say that this method is responsible for 20% of the elapsed time (or 10 seconds) of that thread.

Sampling overhead linearly depends on sampling frequency. Intuitively, you should expect accuracy to be impacted when frequency is lowered. In practice, accuracy depends solely on the amount of samples. 1000 samples are more than enough for most use cases. So, lowering sampling does not impact accuracy. It just takes more time to have enough samples.

### caveats
What sampling cannot tell is how many times a method was invoked. In the above example (specific method is responsible for 10 seconds) a sampler cannot tell if it was: 
- Executed 200 times with average duration of 50ms.
- Executed 20,000 times with average duration of 0.5ms.
- Executed once for 10 consecutive seconds.

### Aggregation and reporting
Taking a snapshot of all threads is pretty easy. Making sense of the data and reporting it in a concise manner to easilly pinpointing the performance problems is the core logic of this sampler.

Each method in the stack trace have two counters:
- **Cumulative time** - How much time was spent inside that method **including** time spent on its called methods.
- **Method time** - How much time was spent inside that method **excluding** time spent on its called methods.

Aggregation is done based on thread name. Meaningful thread naming is key to easily focus on relevant parts of your application.

A thread name manipulator is invoked for each thread every sample. The default thread name manipulator removes digits from thread name. It assumes that parallel worker threads of same executor will share a common prefix and we want aggregate the entire set of worker threads as one.

You may write your own thread name manipulator to allow smarter logic such as replacing thread name with currently executing HTTP request's URL. Such logic might be implemented by maintaining a `ConcurrentHashMap<Thread,String>` and using it for lookups. Alternatively, such logic might be implemented by modifying your application to set thread name as the currently executing URL, User, or anything you like.

In most cases, you are interested in your application code only. E.g. when your code invokes `java.sql.Connection.executeQuery()` interface, a method of the JDBC driver is called. it might called several other methods until the actual wait for DB response. These methods are not interesting. So, the sampler will pile all the cumulative time of executeQuery as if it's "Method time".

You should specify a list of package prefixes that will be considered as "interesting". Whenever the top method in the stack trace not an interesting one, the next methods will be checked until an interesting one is found. 

Additional 
