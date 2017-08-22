# Stage mappings:

| Node operation | Action       | Options    | DependencyMode | ParamMode  | ResultMode     |
| ---            | ---          | ---        | ---            | ---        | ---            |
| Value          | None         |            | None           | None       | Exact          |
| invokeAsync    | Closure      |            | None           | AllDeps    | Exact          |
| acceptEither   | Closure      |            | Any            | FirstDep   | Void           |
| applyToEither  | Closure      |            | Any            | FirstDep   | Exact          |
| exceptionally  | ErrorClosure |            | All            | Error      | Exact          |
| handle         | Closure      |            | All            | SplitFirst | Exact          |
| runAfterBoth   | Closure      |            | All            | None       | Void           |
| runAfterEither | Closure      |            | Any            | None       | Void           |
| thenAccept     | Closure      |            | All            | AllDeps    | Void           |
| thenAcceptBoth | Closure      |            | All            | AllDeps    | Void           |
| thenApply      | Closure      |            | All            | AllDeps    | Exact          |
| thenCombine    | Closure      |            | All            | AllDeps    | Exact          |
| thenCompose    | Closure      |            | All            | AllDeps    | ComposeReplace |
| thenRun        | Closure      |            | All            | None       | Void           |
| whenComplete   | Closure      |            | All            | SplitFirst | Void           |
| anyOf          | Closure      |            | Any            | FirstDep   | Void           |
| allOf          | Closure      |            | All            | AllDeps    | Void           |
| invokeFunction | Call         | fnid, data | All            | None       | Exact          |
| Timer          | Timer        | ms         | None           | None       | Void           |


*   Action : How the stage is executed (it's external effect contract) (options are relevant to action )

    *   None : No action
    *   Closure: Invoke specified closure with resolved parameters
    *   ErrorClosure : Only invoke closure if dependency yields error, otherwise pass-thru value without invoking the closure
    *   Call: Invoke specified function with configured parameters
    *   Timer: Scheduler callback to complete later

*   Dependency Mode: When this node should be triggered, also indicates how errors should propagate

    *   None: It is not triggered by other nodes (Deps are Triggered on start
    *   Single: When exactly one dep is resolved, errors are propagated directly to children.
    *   Any : When any dep is resolved : Propagate last error if all deps fail
    *   All : When deps are resolved : Propagate first error if any deps fai

*   ParamMode : How parameters are built for the action

    *   None : no params , The closure is invoked with no params when triggered
    *   AllDeps : 0-N Params : all (possibly empty) dependencies are resolved in order into the closure
    *   FirstDep : 1+ Params : the result of the first completed dependency is used to invoke the closure
    *   SplitFirst : 1 param : The result of the first dep is split into value/error and these are passed to the closure with null being used for the unused value
    *   Error : 1 param : Only the Error of the triggering node is passed

*   ResultMode : How the result of the closure/action should be exposed as the value of this node

    *   Exact : Take one value from the action and use it directly
    *   Void : Ignore the value of the action and propagate null in its place
    *   ComposeReplace : Process the value of the action as a new completion node by ID , Append this into the graph in place of the current node
