/*
 * Copyright (c) 2010-2012. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.icoin.trading.tradeengine.query.company.repositories;

import com.icoin.trading.tradeengine.domain.model.coin.CoinId;
import com.icoin.trading.tradeengine.query.coin.CoinEntry;
import com.icoin.trading.tradeengine.query.coin.CoinQueryRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.math.BigDecimal;

/**
 * @author Jettro Coenradie
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:META-INF/spring/persistence-infrastructure-context.xml"})
public class CompanyRepositoryIntegrationTest {

    @Autowired
    private CoinQueryRepository coinQueryRepository;

    @Test
    public void storeCompanyInRepository() {
        CoinEntry companyEntry = new CoinEntry();
        companyEntry.setIdentifier(new CoinId().toString());
        companyEntry.setCoinInitialAmount(BigDecimal.valueOf(100000));
        companyEntry.setCoinInitialPrice(BigDecimal.valueOf(1000));
        companyEntry.setTradeStarted(true);

        coinQueryRepository.save(companyEntry);
    }
}