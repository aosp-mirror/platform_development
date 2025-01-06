/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {TraceType} from 'trace/trace_type';
import {AbstractSearchViewFactory} from './abstract_search_view_factory';

export class SearchViewFactoryTransactions extends AbstractSearchViewFactory {
  override readonly traceType = TraceType.TRANSACTIONS;

  override async createSearchViews(): Promise<string[]> {
    const dataTable = await this.createSqlTableWithDefaults(
      'surfaceflinger_transactions',
    );

    const sqlCreateViewTransactionWithProperties = `
      CREATE PERFETTO VIEW transaction_with_properties AS
        SELECT
          STATE.id as state_id,
          STATE.ts,
          TX_ID.display_value as transaction_id,
          PROPERTY.key as property,
          PROPERTY.flat_key as flat_property,
          PROPERTY.display_value as value
        FROM surfaceflinger_transactions STATE
        INNER JOIN ${dataTable} PROPERTY
          ON PROPERTY.base64_proto_id = STATE.base64_proto_id
        LEFT JOIN ${dataTable} TX_ID
          ON TX_ID.base64_proto_id = STATE.base64_proto_id
          AND TX_ID.flat_key = 'transactions.transaction_id'
          AND SUBSTRING(TX_ID.key, 0, instr(TX_ID.key, ']')) = SUBSTRING(PROPERTY.key, 0, instr(PROPERTY.key, ']'));
    `;
    await this.traceProcessor.query(sqlCreateViewTransactionWithProperties);

    const transactionsSearchView = 'transactions_search';
    const sqlCreateViewTransactionSearch = `
      CREATE PERFETTO VIEW ${transactionsSearchView}(
        state_id INT,
        ts INT,
        transaction_id STRING,
        property STRING,
        flat_property STRING,
        value STRING
      ) AS
      SELECT
        TRANS.state_id,
        TRANS.ts,
        TRANS.transaction_id,
        TRANS.property,
        TRANS.flat_property,
        TRANS.value
      FROM surfaceflinger_transactions CURRENT
      INNER JOIN transaction_with_properties TRANS ON CURRENT.id = TRANS.state_id
      ORDER BY TRANS.ts;
    `;
    await this.traceProcessor.query(sqlCreateViewTransactionSearch);
    return [transactionsSearchView];
  }
}
