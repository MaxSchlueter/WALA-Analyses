// Adapted from https://github.com/SRA-SiliconValley/jalangi/blob/master/src/js/analyses/callgraph/CallGraphEngine.js
// Author: Max Schlueter

(function (sandbox) {

    function CallGraphEngine() {

        this.endExecution = function () {
            var cg = Object.create(null);
            for (var callerIid in callerIidToCalleeIidsMap) {
                var caller = iidToFunName[callerIid] + '@' + sandbox.iids[callerIid];
                cg[caller] = [];
                var calleeIids = callerIidToCalleeIidsMap[callerIid];
                for (var i = 0; i < calleeIids.length; i++) {
                    var callee = iidToFunName[calleeIids[i]] + '@' + sandbox.iids[calleeIids[i]];
                    cg[caller].push(callee);
                }
            }

            console.log('Generating call graph...')
            var scriptName = require('path').basename(sandbox.iids.originalCodeFileName, '.js');
            require('fs').writeFileSync('cgs/' + scriptName + '.json', JSON.stringify(cg, undefined, 2), 'utf8');
        }

        var callerIidToCalleeIidsMap = Object.create(null); // caller iid => callee iid
        var iidToFunName = Object.create(null); // function iid => function name

        var callStack = [];

        this.scriptEnter = function (iid, instrumentedFileName, originalFileName) {
            iidToFunName[iid] = require('path').basename(originalFileName);
            callStack.push(iid);
        }

        this.functionEnter = function (iid, fun, dis /* this */, args) {
            var funName = fun.name;
            iidToFunName[iid] = funName;

            var callerIid = callStack[callStack.length - 1];
            if (!(callerIid in callerIidToCalleeIidsMap)) {
                callerIidToCalleeIidsMap[callerIid] = [];
            }

            var calleeIids = callerIidToCalleeIidsMap[callerIid];
            if (calleeIids.indexOf(iid) < 0) {
                calleeIids.push(iid);
            }

            callStack.push(iid);
        }

        this.functionExit = function (iid) {
            callStack.pop();
            return false;
            /* a return of false means that do not backtrack inside the function */
        }

    }

    sandbox.analysis = new CallGraphEngine();
}(J$));
