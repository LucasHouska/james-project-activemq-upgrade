/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.crowdsec;

import static org.apache.james.crowdsec.CrowdsecExtension.CROWDSEC_PORT;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URL;

import org.apache.james.crowdsec.client.CrowdsecClientConfiguration;
import org.apache.james.protocols.smtp.SMTPSession;
import org.apache.james.protocols.smtp.hook.HookResult;
import org.apache.james.protocols.smtp.utils.BaseFakeSMTPSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class CrowdsecEhloHookTest {
    private static CrowdsecEhloHook ehloHook;

    @RegisterExtension
    static CrowdsecExtension crowdsecExtension = new CrowdsecExtension();

    @BeforeEach
    void setUpEach() throws IOException {
        int port = crowdsecExtension.getCrowdsecContainer().getMappedPort(CROWDSEC_PORT);
        ehloHook = new CrowdsecEhloHook(new CrowdsecClientConfiguration(new URL("http://localhost:" + port + "/v1"), CrowdsecClientConfiguration.DEFAULT_API_KEY));
    }

    @Test
    void givenIPBannedByCrowdsecDecisionIp() throws IOException, InterruptedException {
        banIP("--ip", "127.0.0.1");
        SMTPSession session = new BaseFakeSMTPSession() {};

        assertThat(ehloHook.doHelo(session, "localhost")).isEqualTo(HookResult.DENY);
    }

    @Test
    void givenIPBannedByCrowdsecDecisionIpRange() throws IOException, InterruptedException {
        banIP("--range", "127.0.0.1/24");
        SMTPSession session = new BaseFakeSMTPSession() {};

        assertThat(ehloHook.doHelo(session, "localhost")).isEqualTo(HookResult.DENY);
    }

    @Test
    void givenIPNotBannedByCrowdsecDecisionIp() throws IOException, InterruptedException {
        banIP("--ip", "192.182.39.2");
        SMTPSession session = new BaseFakeSMTPSession() {};

        assertThat(ehloHook.doHelo(session, "localhost")).isEqualTo(HookResult.DECLINED);
    }

    @Test
    void givenIPNotBannedByCrowdsecDecisionIpRange() throws IOException, InterruptedException {
        banIP("--range", "192.182.39.2/24");
        SMTPSession session = new BaseFakeSMTPSession() {};

        assertThat(ehloHook.doHelo(session, "localhost")).isEqualTo(HookResult.DECLINED);
    }

    private static void banIP(String type, String value) throws IOException, InterruptedException {
        crowdsecExtension.getCrowdsecContainer().execInContainer("cscli", "decision", "add", type, value);
    }
}
