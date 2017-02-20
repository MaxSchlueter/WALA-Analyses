# Compare Call Graphs
Compare the dynamic call graph obtained from a Jalangi2 analysis to the static call graph of a JavaScript file in WALA. The `ComputeCGAccuracy.java` analysis measures the call graph accuracy of the static call graph with respect to the dynamic one by computing the average precision and recall. For every call site covered by the dynamic call graph, the precision is the percentage of "true" call targets and recall is the percentage of correctly identified true targets. A low precision measure usually means that the static call graph contains many false positives and a recall measure < 100% means that the static call graph is missing call sites/targets, hence the static analysis is not sound.

# Example
After having imported the project into your WALA workspace, go to the `launchers` directory and run `ComputeCGAccuracy.launch` as a Java application. The last three lines of the console output should look like this:

```
Computing precision and recall for file: md5-bundler.js
Average precision: 0.947530864197531
Average recall:    1.0
```

The application built the static call graph of `tests/md5-bundler.js` in WALA and compared it to the dynamic call graph of that file found in `jalangi/cgs/md5-bundler.json`. 

# Usage
1. Copy the JavaScript file you want to analyze, say `foo.js`, to the `tests` directory

2. Compute the dynamic call graph of the file by invoking the Jalangi2 analysis:

    ```
    $ cd jalangi/
    $ node ${JALANGI_HOME}/src/js/commands/jalangi.js --inlineIID --analysis CallGraph.js ../tests/foo.js
    ```

3. Go to the run configuration of `ComputeCGAccuracy.launch` and change the first argument to: `tests/foo.js`

Alternatively specify the path to the JavaScript file as the first argument in the run configuration and the path to the Jalangi call graph of that file as second argument.
