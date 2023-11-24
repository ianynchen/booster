package io.github.booster.web.handler.compression;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public interface CompressionTestData {

    String TEXT_TO_COMPRESS =
            "Planning for disaster recovery and zero downtime means having a replica set of the MongoDB cluster across a single region or a number of regions, depending on how crucial the assessment of the risk is.\n" +
                    "\nA replica set consists of the primary mongod process and other secondary processes. It is recommended that the total number of these processes is odd so that majority is ensured in the case that the master fails and a new master needs to be allocated.\n" +
                    "\nSharding a MongoDB cluster is also at the cornerstone of deploying a production cluster with huge data loads.\n" + "\nObviously, designing your data models, appropriately storing them in collections, and defining corrected indexes is essential. But if you truly want to leverage the power of MongoDB, you need to have a plan regarding sharding your cluster.";

    String DEFLATE_COMPRESSED =
            "eJxdU0GS1DAMvO8r9ICt+QNbXDhQUMULNJaSGGxrynImhNfTtmd2C46Ru1utlvI9cSmxrLRYJYnO3rRS1WB3rSdxEfqj1UjsKC1mpaxcnDa+dxIDeUsxMLk2soXapvTVymqf3yikfYhxqOYOrIOSFJQ1WiH0Yyp7vgIC5qz6K4netEhXB2izg0LdQ+Q0tNld3bOW9241+i+Kfnl5+fSPmQCx6M2fuFuNmTFR7u4Enxa0u8KAhvcKEijSIY839Qt9adAecWQ0FRVocRuCzRpMfQyAmusHt/NMhNwmJfNPq7Gdva7F9wqtWIZSYBDfdfNcwcIxTXtISY9nuaiKozddEUZKFripYPgfG1eZK/k/fzTkBBsP/WC1aPVmRbttxJ3snEyYlz20vp0n+Yhto21flYQbUzKWnvW36z3a7uns+/K4jhM6ba8Tlk00YZd8gySSh8l0EnrWjoOL3GcPlpKOdh2KSUWXOJTgEZm3EZHo75lmjxU3yOlCbzv2svSG1OoO6YNxEUglKc6WV50rt2Ou5pHI6yD0BDsUJ6x9ZvwA/fge8fkzxzHMI4XLX/+YJbo=";

    String GZIP_COMPRESSED =
            "H4sIAAAAAAAA/11TQZLUMAy87yv0gK35A1tcOFBQxQs0lpIYbGvKciaE19O2Z3YLjpG7W62W8j1xKbGstFglic7etFLVYHetJ3ER+qPVSOwoLWalrFycNr53EgN5SzEwuTayhdqm9NXKap/fKKR9iHGo5g6sg5IUlDVaIfRjKnu+AgLmrPorid60SFcHaLODQt1D5DS02V3ds5b3bjX6L4p+eXn59I+ZALHozZ+4W42ZMVHu7gSfFrS7woCG9woSKNIhjzf1C31p0B5xZDQVFWhxG4LNGkx9DICa6we380yE3CYl80+rsZ29rsX3Cq1YhlJgEN9181zBwjFNe0hJj2e5qIqjN10RRkoWuKlg+B8bV5kr+T9/NOQEGw/9YLVo9WZFu23EneycTJiXPbS+nSf5iG2jbV+VhBtTMpae9bfrPdru6ez78riOEzptrxOWTTRhl3yDJJKHyXQSetaOg4vcZw+Wko52HYpJRZc4lOARmbcRkejvmWaPFTfI6UJvO/ay9IbU6g7pg3ERSCUpzpZXnSu3Y67mkcjrIPQEOxQnrH1m/AD9+B7x+TPHMcwjhctfOKoWlyYDAAA=";

    String BROTLI_COMPRESSED =
            "GyUDIMTa3LQeu7QM0toyPP1lwufLa4iqbYTQum1vyTyKgMMiCPjmhESod+a304alc6mIfHKKMAYqkeRiV6wHA6MqIW+RGOlp0igAwb/BbvUWDE6It/flrfiZnXKKNdEKQ2D9wgMo8WFQzD205+sLMiCOmwlqY4EOSnhmPDbovT8AVwU2ki4KZRUn0mzKhlxfFQ9FcdWB1mdtMn93VR/i2Fi/mOVD8RdffVqmGVhZEF6CxvUm7YZVTC6MbbeCzNgq8yZwf9pFiHA52uWh7OY7cygQH6kOOBcmIwgV6PZUVqfthDvOII0zpVbIzHCzGeuGvS3Bh5a2ulEL32QV6V0IrbXyb/mfqE+Z7KrZ7G/jxk0KA183UjrlgUXt4E/Bp9Vh6oHKqo8R0Qc=";

    String LZW_COMPRESSED =
            "JgMAAPArUGxhbm5pbmcgZm9yIGRpc2FzdGVyIHJlY292ZXJ5IGFuZCB6ZXJvIGRvd250aW1lIG1lYW5zIGhhdjUA8RFhIHJlcGxpY2Egc2V0IG9mIHRoZSBNb25nb0RCIGNsdU0A8BNhY3Jvc3MgYSBzaW5nbGUgcmVnaW9uIG9yIGEgbnVtYmVyOgACFgCQcywgZGVwZW5kXgDhb24gaG93IGNydWNpYWxcAJVhc3Nlc3NtZW5uALlyaXNrIGlzLgoKQY0AgGNvbnNpc3RzXAAAlgCQcHJpbWFyeSBtngBwZCBwcm9jZZYA8ABuZCBvdGhlciBzZWNvbmQjAAMcAJFlcy4gSXQgaXMPAcFtbWVuZGVkIHRoYXSLAFd0b3RhbMEAQHRoZXNnAAFYACBlczgAYm9kZCBzbzMAgG1ham9yaXR5GAChZW5zdXJlZCBpbkoAQmNhc2UlAACpABJtgwFCZmFpbJ8AVGEgbmV3FwDxDW5lZWRzIHRvIGJlIGFsbG9jYXRlZC4KClNoYXI1AR1hgAGDaXMgYWxzbyC4ALBjb3JuZXJzdG9uZRwBYGRlcGxveW8BEGEVAUBkdWN0ngEEvQHwBHdpdGggaHVnZSBkYXRhIGxvYWRsAZBPYnZpb3VzbHmwATFzaWdDAkJ5b3VyJwDwC21vZGVscywgYXBwcm9wcmlhdGVseSBzdG9yaQBAdGhlbfwAUWNvbGxlbwAAKwBxbmQgZGVmaUoAcWNvcnJlY3QjATJkZXhOAfARZXNzZW50aWFsLiBCdXQgaWYgeW91IHRydWx5IHdhbnQWAXBsZXZlcmFnTwFSZSBwb3dcAhBN9AExREIsMQAAQgEAKwCwaGF2ZSBhIHBsYW6TAgM/ARRzSAEBzQCAY2x1c3Rlci4=";

    static byte[] getUtfBytes(String str) {
        return str.getBytes(StandardCharsets.UTF_8);
    }

    static String encode(byte[] bytes) {
        return new String(Base64.getEncoder().encode(bytes), StandardCharsets.UTF_8);
    }

    static byte[] decode(byte[] bytes) {
        return Base64.getDecoder().decode(bytes);
    }

    static byte[] decode(String str) {
        return Base64.getDecoder().decode(str.getBytes(StandardCharsets.UTF_8));
    }
}
