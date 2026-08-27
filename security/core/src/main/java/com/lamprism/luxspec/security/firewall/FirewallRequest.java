/*
 * Copyright (C) Lamprism
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

package com.lamprism.luxspec.security.firewall;

/**
 * Marks request facts that can be consumed by transport-independent firewall rules.
 *
 * <p>This contract intentionally contains no transport-specific fields. Each transport can provide
 * its own request facts without supplying placeholders for another protocol. The generic
 * {@link FirewallRule} contract does not require this marker, so a rule may also use a dedicated
 * request type when that is the appropriate boundary.</p>
 *
 * @author RollW
 */
public interface FirewallRequest {
}
