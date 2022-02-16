/*
 * Copyright (c) 2008-2021, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.client.impl.protocol.codec;

import com.hazelcast.client.impl.protocol.ClientMessage;
import com.hazelcast.client.impl.protocol.Generated;
import com.hazelcast.client.impl.protocol.codec.builtin.*;
import com.hazelcast.client.impl.protocol.codec.custom.*;

import javax.annotation.Nullable;

import static com.hazelcast.client.impl.protocol.ClientMessage.*;
import static com.hazelcast.client.impl.protocol.codec.builtin.FixedSizeTypesCodec.*;

/*
 * This file is auto-generated by the Hazelcast Client Protocol Code Generator.
 * To change this file, edit the templates or the protocol
 * definitions on the https://github.com/hazelcast/hazelcast-client-protocol
 * and regenerate it.
 */

/**
 * Updates the configuration on the member with the passed configuration, applies (merges) the passed config to the current config
 */
@Generated("66c3b43feec0bd30445a3f33d9a26572")
public final class MCUpdateConfigCodec {
    //hex: 0x202300
    public static final int REQUEST_MESSAGE_TYPE = 2106112;
    //hex: 0x202301
    public static final int RESPONSE_MESSAGE_TYPE = 2106113;
    private static final int REQUEST_INITIAL_FRAME_SIZE = PARTITION_ID_FIELD_OFFSET + INT_SIZE_IN_BYTES;
    private static final int RESPONSE_CONFIG_UPDATE_PROCESS_ID_FIELD_OFFSET = RESPONSE_BACKUP_ACKS_FIELD_OFFSET + BYTE_SIZE_IN_BYTES;
    private static final int RESPONSE_INITIAL_FRAME_SIZE = RESPONSE_CONFIG_UPDATE_PROCESS_ID_FIELD_OFFSET + UUID_SIZE_IN_BYTES;

    private MCUpdateConfigCodec() {
    }

    public static ClientMessage encodeRequest(java.lang.String configuration) {
        ClientMessage clientMessage = ClientMessage.createForEncode();
        clientMessage.setRetryable(false);
        clientMessage.setOperationName("MC.UpdateConfig");
        ClientMessage.Frame initialFrame = new ClientMessage.Frame(new byte[REQUEST_INITIAL_FRAME_SIZE], UNFRAGMENTED_MESSAGE);
        encodeInt(initialFrame.content, TYPE_FIELD_OFFSET, REQUEST_MESSAGE_TYPE);
        encodeInt(initialFrame.content, PARTITION_ID_FIELD_OFFSET, -1);
        clientMessage.add(initialFrame);
        StringCodec.encode(clientMessage, configuration);
        return clientMessage;
    }

    /**
     * the config patch (XML or YAML string) to be applied to the current configuration
     */
    public static java.lang.String decodeRequest(ClientMessage clientMessage) {
        ClientMessage.ForwardFrameIterator iterator = clientMessage.frameIterator();
        //empty initial frame
        iterator.next();
        return StringCodec.decode(iterator);
    }

    public static ClientMessage encodeResponse(java.util.UUID configUpdateProcessId) {
        ClientMessage clientMessage = ClientMessage.createForEncode();
        ClientMessage.Frame initialFrame = new ClientMessage.Frame(new byte[RESPONSE_INITIAL_FRAME_SIZE], UNFRAGMENTED_MESSAGE);
        encodeInt(initialFrame.content, TYPE_FIELD_OFFSET, RESPONSE_MESSAGE_TYPE);
        encodeUUID(initialFrame.content, RESPONSE_CONFIG_UPDATE_PROCESS_ID_FIELD_OFFSET, configUpdateProcessId);
        clientMessage.add(initialFrame);

        return clientMessage;
    }

    /**
     * The unique identifier of the configuration update process which will be included in all MCEvent instances emitted for MC during the update
     */
    public static java.util.UUID decodeResponse(ClientMessage clientMessage) {
        ClientMessage.ForwardFrameIterator iterator = clientMessage.frameIterator();
        ClientMessage.Frame initialFrame = iterator.next();
        return decodeUUID(initialFrame.content, RESPONSE_CONFIG_UPDATE_PROCESS_ID_FIELD_OFFSET);
    }
}
