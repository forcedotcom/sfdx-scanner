package com.salesforce.messaging;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class CliMessagerTest {

    private static final PrintStream ORIGINAL_STDOUT = System.out;

    @AfterEach
    public void teardown() {
        System.setOut(ORIGINAL_STDOUT);
    }

    @Test
    public void verifyPostMessage_withWellFormedInput() {
        ByteArrayOutputStream testOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(testOut));

        CliMessager.postMessage("TestInternalLog", EventKey.INFO_TELEMETRY, "Arg1");
        assertThat("Message is properly bounded at start", testOut.toString().trim().startsWith("SFCA-REALTIME-START"), is(true));
        assertThat("Message is properly bounded at end", testOut.toString().trim().endsWith("SFCA-REALTIME-END"), is(true));
        assertThat("Message has proper type attribute", testOut.toString().contains("\"type\":\"TELEMETRY\""), is(true));
        assertThat("Message has proper args attribute", testOut.toString().contains("\"args\":[\"Arg1\"]"), is(true));
        assertThat("Message has proper internalLog attribute", testOut.toString().contains("\"internalLog\":\"TestInternalLog\""), is(true));
    }

    @Test
    public void verifyPostMessage_withMalformedInput() {
        AssertionError err = assertThrows(AssertionError.class, () -> {
            CliMessager.postMessage("TestInternalLog", EventKey.INFO_TELEMETRY, "Arg1", "Arg2");
        });

        assertThat("Assertion violated", err.getMessage().equals("EventKey expected 1 args, received 2"), is(true));
    }
}
