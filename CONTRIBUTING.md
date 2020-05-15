# Contributing to the Fn Java FDK

We welcome all contributions!

## How to contribute
   * Fork the repo.
   * Fix an issue. Or create an issue and fix it.
   * Create a pull request. Reference the issue that you are fixing.
   * Your branch will be built by our CI system. Make sure that this build passes.
   * Be ready to respond to comments and questions from the community.
   * Good job! Thanks for being awesome!
   
## Tips for a swift merge
   * Make sure your branch has no conflicts with master. Rebase often.
   * Ensure that you have updated any JavaDoc or user/technical docs in `/docs`.
   * Don't make breaking changes to the public APIs.
   * Write tests - especially for public APIs.
   * Make sure that changes to `api` are backwards compatible with `runtime` and vice-versa.
   
## Note for mac users
   #### If you run into build failures, try the steps below:
   * Install `cmake`, if you don't have it already by running `brew install cmake`
   * Go to `fdk-java/runtime/src/main/c` folder and run `./buildit.sh`
  
