package me.daskabel.dummy2pro.tools;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class BcryptSmokeTest
{
    @Test
    void main_printsHashAndSuccessfulMatch()
    {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        try
        {
            System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
            Bcrypt.main(new String[0]);
        }
        finally
        {
            System.setOut(originalOut);
        }

        String output = buffer.toString(StandardCharsets.UTF_8);

        assertTrue(output.contains("hash="));
        assertTrue(output.contains("matches=true"));
    }
}