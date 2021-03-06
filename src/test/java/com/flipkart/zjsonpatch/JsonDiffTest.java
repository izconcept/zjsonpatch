/*
 * Copyright 2016 flipkart.com zjsonpatch.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package com.flipkart.zjsonpatch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.Random;

/**
 * Unit test
 */
public class JsonDiffTest {
    static ObjectMapper objectMapper = new ObjectMapper();
    static ArrayNode jsonNode;

    @BeforeClass
    public static void beforeClass() throws IOException {
        String path = "/testdata/sample.json";
        InputStream resourceAsStream = JsonDiffTest.class.getResourceAsStream(path);
        String testData = IOUtils.toString(resourceAsStream, "UTF-8");
        jsonNode = (ArrayNode) objectMapper.readTree(testData);
    }

    @Test
    public void testSampleJsonDiff() throws Exception {
        for (int i = 0; i < jsonNode.size(); i++) {
            JsonNode first = jsonNode.get(i).get("first");
            JsonNode second = jsonNode.get(i).get("second");

//            System.out.println("Test # " + i);
//            System.out.println(first);
//            System.out.println(second);

            JsonNode actualPatch = JsonDiff.asJson(first, second);


//            System.out.println(actualPatch);

            JsonNode secondPrime = JsonPatch.apply(actualPatch, first);
//            System.out.println(secondPrime);
            Assert.assertTrue(second.equals(secondPrime));
        }
    }

    @Test
    public void testGeneratedJsonDiff() throws Exception {
        Random random = new Random();
        for (int i = 0; i < 1000; i++) {
            JsonNode first = TestDataGenerator.generate(random.nextInt(10));
            JsonNode second = TestDataGenerator.generate(random.nextInt(10));

            JsonNode actualPatch = JsonDiff.asJson(first, second);
//            System.out.println("Test # " + i);
//
//            System.out.println(first);
//            System.out.println(second);
//            System.out.println(actualPatch);

            JsonNode secondPrime = JsonPatch.apply(actualPatch, first);
//            System.out.println(secondPrime);
            Assert.assertTrue(second.equals(secondPrime));
        }
    }

    @Test
    public void newPOCTest() {
        JsonNode source = jsonNode.get(jsonNode.size()-1).get("first");
        JsonNode target = jsonNode.get(jsonNode.size()-1).get("second");
        System.out.println(source);
        System.out.println(target);

        System.out.println("\n--------Diff--------");
        JsonNode diff = JsonDiff.asJson(source, target);
        for (JsonNode node: diff) {
            System.out.println(node);
        }
    }

    @Test
    public void testRenderedRemoveOperationOmitsValueByDefault() throws Exception {
        ObjectNode source = objectMapper.createObjectNode();
        ObjectNode target = objectMapper.createObjectNode();
        source.put("field", "value");

        JsonNode diff = JsonDiff.asJson(source, target);

        Assert.assertEquals(Operation.REMOVE.rfcName(), diff.get(0).get("op").textValue());
        Assert.assertEquals("/field", diff.get(0).get("path").textValue());
        Assert.assertNull(diff.get(0).get("value"));
    }

    @Test
    public void testRenderedRemoveOperationRetainsValueIfOmitDiffFlagNotSet() throws Exception {
        ObjectNode source = objectMapper.createObjectNode();
        ObjectNode target = objectMapper.createObjectNode();
        source.put("field", "value");

        EnumSet<DiffFlags> flags = DiffFlags.defaults().clone();
        Assert.assertTrue("Expected OMIT_VALUE_ON_REMOVE by default", flags.remove(DiffFlags.OMIT_VALUE_ON_REMOVE));
        JsonNode diff = JsonDiff.asJson(source, target, flags);

        Assert.assertEquals(Operation.REMOVE.rfcName(), diff.get(0).get("op").textValue());
        Assert.assertEquals("/field", diff.get(0).get("path").textValue());
        Assert.assertEquals("value", diff.get(0).get("value").textValue());
    }

    @Test
    public void testRenderedOperationsExceptMoveAndCopy() throws Exception {
        JsonNode source = objectMapper.readTree("{\"age\": 10}");
        JsonNode target = objectMapper.readTree("{\"height\": 10}");

        EnumSet<DiffFlags> flags = DiffFlags.dontNormalizeOpIntoMoveAndCopy().clone(); //only have ADD, REMOVE, REPLACE, Don't normalize operations into MOVE & COPY

        JsonNode diff = JsonDiff.asJson(source, target, flags);

//        System.out.println(source);
//        System.out.println(target);
//        System.out.println(diff);

        for (JsonNode d : diff) {
            Assert.assertNotEquals(Operation.MOVE.rfcName(), d.get("op").textValue());
            Assert.assertNotEquals(Operation.COPY.rfcName(), d.get("op").textValue());
        }

        JsonNode targetPrime = JsonPatch.apply(diff, source);
//        System.out.println(targetPrime);
        Assert.assertTrue(target.equals(targetPrime));


    }

    @Test
    public void testPath() throws Exception {
        JsonNode source = objectMapper.readTree("{\"profiles\":{\"abc\":[],\"def\":[{\"hello\":\"world\"}]}}");
        JsonNode patch = objectMapper.readTree("[{\"op\":\"copy\",\"from\":\"/profiles/def/0\", \"path\":\"/profiles/def/0\"},{\"op\":\"replace\",\"path\":\"/profiles/def/0/hello\",\"value\":\"world2\"}]");

        JsonNode target = JsonPatch.apply(patch, source);
//        System.out.println(target);
        JsonNode expected = objectMapper.readTree("{\"profiles\":{\"abc\":[],\"def\":[{\"hello\":\"world2\"},{\"hello\":\"world\"}]}}");
        Assert.assertTrue(target.equals(expected));
    }
}
