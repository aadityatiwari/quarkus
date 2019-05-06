/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package io.quarkus.it.reactive.pg.client;

import java.util.concurrent.CompletionStage;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.reactiverse.axle.pgclient.PgIterator;
import io.reactiverse.axle.pgclient.PgPool;
import io.reactiverse.axle.pgclient.Row;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@Path("/fruits")
public class FruitResource {

    @Inject
    PgPool client;

    @PostConstruct
    void setupDb() {
        client.query("DROP TABLE IF EXISTS fruits")
                .thenCompose(r -> client.query("CREATE TABLE fruits (id SERIAL PRIMARY KEY, name TEXT NOT NULL)"))
                .thenCompose(r -> client.query("INSERT INTO fruits (name) VALUES ('Orange')"))
                .thenCompose(r -> client.query("INSERT INTO fruits (name) VALUES ('Pear')"))
                .thenCompose(r -> client.query("INSERT INTO fruits (name) VALUES ('Apple')"))
                .toCompletableFuture()
                .join();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public CompletionStage<JsonArray> listFruits() {
        return client.query("SELECT * FROM fruits")
                .thenApply(pgRowSet -> {
                    JsonArray jsonArray = new JsonArray();
                    PgIterator iterator = pgRowSet.iterator();
                    while (iterator.hasNext()) {
                        Row row = iterator.next();
                        jsonArray.add(toJson(row));
                    }
                    return jsonArray;
                });
    }

    private JsonObject toJson(Row row) {
        return new JsonObject()
                .put("id", row.getLong("id"))
                .put("name", row.getString("name"));
    }

}
