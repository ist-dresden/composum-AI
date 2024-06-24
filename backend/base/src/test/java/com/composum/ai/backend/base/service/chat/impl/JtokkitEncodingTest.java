package com.composum.ai.backend.base.service.chat.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import com.knuddels.jtokkit.api.IntArrayList;

/**
 * Play around with encoding.
 */
public class JtokkitEncodingTest {

    protected EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
    protected Encoding enc = registry.getEncoding(EncodingType.CL100K_BASE);

    @Test
    public void testTokenization() {
        String text = "hallo";
        IntArrayList encoded = enc.encode(text);
        // make a list of strings that correspond to the tokens
        List<String> decoded = new ArrayList<>();
        for (int i = 0; i < encoded.size(); i++) {
            IntArrayList token = new IntArrayList();
            token.add(encoded.get(i));
            decoded.add(enc.decode(token));
        }
        Assert.assertEquals(Arrays.asList("hal", "lo"), decoded);
    }

}
