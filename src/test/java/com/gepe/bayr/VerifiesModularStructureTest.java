package com.gepe.bayr;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

public class VerifiesModularStructureTest {
    @Test
    void verifiesModularStructure() {
        ApplicationModules.of(BayrApplication.class).verify();
    }
}
