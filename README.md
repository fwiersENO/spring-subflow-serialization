Spring subflow serializaztion with defaultOutputToParentFlow

A test showing the difference in behavior when a Spring integration
subflow is created wiht and without "defaultOutputToParentFlow".

For some reason, using the Spring cloud annotation "EnableBinding"
and using a subflow with "defaultOutputToParentFlow"
causes the payload of the message to be serialized.

This test uses a payload that cannot (and should not) be serialized,
serialization either causes a "Stackoverflow" exception
(when running the test using the command line "mvn clean package")
or causes a "MessageConversionException: Could not write JSON".

Looking for a solution where "EnableBinding" can be used
together with subflows with "defaultOutputToParentFlow"
and message payloads are not serialized.
