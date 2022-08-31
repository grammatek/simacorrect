The tests can essentially be split into two types. Tests that are read from the "YfirlesturAnnotationTestCases.json" where only input and output is required and then we have tests which have their input / output defined in the test itself amongst other data required to test the specified behavior. One can add an indefinite amount of test cases to the [json](./res/YfirlesturAnnotationTestCases.json) file for a more comprehensive coverage.


The tests are run against a [Yfirlestur's web service](https://github.com/mideind/Yfirlestur) which one can run themselves locally or against the provider of the web service. Information as to how to setup that service or which endpoint you can use is available in the link.

By default the tests are configured to run against a local service which is set up in that way for the CI. The service url can be swapped out in the tests:
     
    # default
    private val api = DevelopersApi("http://localhost:5002")

    # if one doesn't have / want a local service, one can use one of either:
    private val api = DevelopersApi("https://yfirlestur.grammatek.com")
    private val api = DevelopersApi("https://yfirlestur.is")


To run the tests locally via the command line:

    `./gradlew test`

The tests are also ran in the [CI](../../../.github/workflows/build.yml) where it's tested against a local Yfirlestur service.