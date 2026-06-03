package com.bragdev.frauddetection;

import com.bragdev.frauddetection.common.enums.ModelStatus;
import com.bragdev.frauddetection.common.model.ModelVersion;
import com.bragdev.frauddetection.common.repository.ModelVersionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the FRAUD-124 actuator hardening over real HTTP (through the security filter chain):
 * liveness/readiness probes are exposed, readiness genuinely reflects the model-loaded state, and
 * anonymous callers never see component details.
 */
class ActuatorProbesIT extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ModelVersionRepository modelVersionRepository;

    @Test
    void livenessProbeIsPublicAndUp() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
    }

    @Test
    void readinessIsUpOnceAModelIsDeployed() throws Exception {
        // db + redis are healthy (Testcontainers); deploying a model satisfies the modelRegistry gate.
        if (modelVersionRepository.findByStatus(ModelStatus.DEPLOYED).isEmpty()) {
            modelVersionRepository.save(ModelVersion.builder()
                    .version("readiness-it-" + UUID.randomUUID())
                    .status(ModelStatus.DEPLOYED)
                    .deployedAt(Instant.now())
                    .build());
        }

        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
    }

    @Test
    void anonymousHealthHidesComponentDetails() throws Exception {
        // show-details/show-components=when_authorized -> anonymous sees only the aggregate status.
        mockMvc.perform(get("/actuator/health"))
                .andExpect(content().string(not(org.hamcrest.Matchers.containsString("components"))));
    }
}
