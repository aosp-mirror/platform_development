/*eslint-disable block-scoped-var, id-length, no-control-regex, no-magic-numbers, no-prototype-builtins, no-redeclare, no-shadow, no-var, sort-vars*/
import * as $protobuf from "protobufjs/minimal";

// Common aliases
const $Reader = $protobuf.Reader, $Writer = $protobuf.Writer, $util = $protobuf.util;

// Exported root namespace
const $root = $protobuf.roots["default"] || ($protobuf.roots["default"] = {});

export const chromeos_update_engine = $root.chromeos_update_engine = (() => {

    /**
     * Namespace chromeos_update_engine.
     * @exports chromeos_update_engine
     * @namespace
     */
    const chromeos_update_engine = {};

    chromeos_update_engine.Extent = (function() {

        /**
         * Properties of an Extent.
         * @memberof chromeos_update_engine
         * @interface IExtent
         * @property {number|Long|null} [startBlock] Extent startBlock
         * @property {number|Long|null} [numBlocks] Extent numBlocks
         */

        /**
         * Constructs a new Extent.
         * @memberof chromeos_update_engine
         * @classdesc Represents an Extent.
         * @implements IExtent
         * @constructor
         * @param {chromeos_update_engine.IExtent=} [properties] Properties to set
         */
        function Extent(properties) {
            if (properties)
                for (let keys = Object.keys(properties), i = 0; i < keys.length; ++i)
                    if (properties[keys[i]] != null)
                        this[keys[i]] = properties[keys[i]];
        }

        /**
         * Extent startBlock.
         * @member {number|Long} startBlock
         * @memberof chromeos_update_engine.Extent
         * @instance
         */
        Extent.prototype.startBlock = $util.Long ? $util.Long.fromBits(0,0,true) : 0;

        /**
         * Extent numBlocks.
         * @member {number|Long} numBlocks
         * @memberof chromeos_update_engine.Extent
         * @instance
         */
        Extent.prototype.numBlocks = $util.Long ? $util.Long.fromBits(0,0,true) : 0;

        /**
         * Creates a new Extent instance using the specified properties.
         * @function create
         * @memberof chromeos_update_engine.Extent
         * @static
         * @param {chromeos_update_engine.IExtent=} [properties] Properties to set
         * @returns {chromeos_update_engine.Extent} Extent instance
         */
        Extent.create = function create(properties) {
            return new Extent(properties);
        };

        /**
         * Encodes the specified Extent message. Does not implicitly {@link chromeos_update_engine.Extent.verify|verify} messages.
         * @function encode
         * @memberof chromeos_update_engine.Extent
         * @static
         * @param {chromeos_update_engine.IExtent} message Extent message or plain object to encode
         * @param {$protobuf.Writer} [writer] Writer to encode to
         * @returns {$protobuf.Writer} Writer
         */
        Extent.encode = function encode(message, writer) {
            if (!writer)
                writer = $Writer.create();
            if (message.startBlock != null && Object.hasOwnProperty.call(message, "startBlock"))
                writer.uint32(/* id 1, wireType 0 =*/8).uint64(message.startBlock);
            if (message.numBlocks != null && Object.hasOwnProperty.call(message, "numBlocks"))
                writer.uint32(/* id 2, wireType 0 =*/16).uint64(message.numBlocks);
            return writer;
        };

        /**
         * Encodes the specified Extent message, length delimited. Does not implicitly {@link chromeos_update_engine.Extent.verify|verify} messages.
         * @function encodeDelimited
         * @memberof chromeos_update_engine.Extent
         * @static
         * @param {chromeos_update_engine.IExtent} message Extent message or plain object to encode
         * @param {$protobuf.Writer} [writer] Writer to encode to
         * @returns {$protobuf.Writer} Writer
         */
        Extent.encodeDelimited = function encodeDelimited(message, writer) {
            return this.encode(message, writer).ldelim();
        };

        /**
         * Decodes an Extent message from the specified reader or buffer.
         * @function decode
         * @memberof chromeos_update_engine.Extent
         * @static
         * @param {$protobuf.Reader|Uint8Array} reader Reader or buffer to decode from
         * @param {number} [length] Message length if known beforehand
         * @returns {chromeos_update_engine.Extent} Extent
         * @throws {Error} If the payload is not a reader or valid buffer
         * @throws {$protobuf.util.ProtocolError} If required fields are missing
         */
        Extent.decode = function decode(reader, length) {
            if (!(reader instanceof $Reader))
                reader = $Reader.create(reader);
            let end = length === undefined ? reader.len : reader.pos + length, message = new $root.chromeos_update_engine.Extent();
            while (reader.pos < end) {
                let tag = reader.uint32();
                switch (tag >>> 3) {
                case 1:
                    message.startBlock = reader.uint64();
                    break;
                case 2:
                    message.numBlocks = reader.uint64();
                    break;
                default:
                    reader.skipType(tag & 7);
                    break;
                }
            }
            return message;
        };

        /**
         * Decodes an Extent message from the specified reader or buffer, length delimited.
         * @function decodeDelimited
         * @memberof chromeos_update_engine.Extent
         * @static
         * @param {$protobuf.Reader|Uint8Array} reader Reader or buffer to decode from
         * @returns {chromeos_update_engine.Extent} Extent
         * @throws {Error} If the payload is not a reader or valid buffer
         * @throws {$protobuf.util.ProtocolError} If required fields are missing
         */
        Extent.decodeDelimited = function decodeDelimited(reader) {
            if (!(reader instanceof $Reader))
                reader = new $Reader(reader);
            return this.decode(reader, reader.uint32());
        };

        /**
         * Verifies an Extent message.
         * @function verify
         * @memberof chromeos_update_engine.Extent
         * @static
         * @param {Object.<string,*>} message Plain object to verify
         * @returns {string|null} `null` if valid, otherwise the reason why it is not
         */
        Extent.verify = function verify(message) {
            if (typeof message !== "object" || message === null)
                return "object expected";
            if (message.startBlock != null && message.hasOwnProperty("startBlock"))
                if (!$util.isInteger(message.startBlock) && !(message.startBlock && $util.isInteger(message.startBlock.low) && $util.isInteger(message.startBlock.high)))
                    return "startBlock: integer|Long expected";
            if (message.numBlocks != null && message.hasOwnProperty("numBlocks"))
                if (!$util.isInteger(message.numBlocks) && !(message.numBlocks && $util.isInteger(message.numBlocks.low) && $util.isInteger(message.numBlocks.high)))
                    return "numBlocks: integer|Long expected";
            return null;
        };

        /**
         * Creates an Extent message from a plain object. Also converts values to their respective internal types.
         * @function fromObject
         * @memberof chromeos_update_engine.Extent
         * @static
         * @param {Object.<string,*>} object Plain object
         * @returns {chromeos_update_engine.Extent} Extent
         */
        Extent.fromObject = function fromObject(object) {
            if (object instanceof $root.chromeos_update_engine.Extent)
                return object;
            let message = new $root.chromeos_update_engine.Extent();
            if (object.startBlock != null)
                if ($util.Long)
                    (message.startBlock = $util.Long.fromValue(object.startBlock)).unsigned = true;
                else if (typeof object.startBlock === "string")
                    message.startBlock = parseInt(object.startBlock, 10);
                else if (typeof object.startBlock === "number")
                    message.startBlock = object.startBlock;
                else if (typeof object.startBlock === "object")
                    message.startBlock = new $util.LongBits(object.startBlock.low >>> 0, object.startBlock.high >>> 0).toNumber(true);
            if (object.numBlocks != null)
                if ($util.Long)
                    (message.numBlocks = $util.Long.fromValue(object.numBlocks)).unsigned = true;
                else if (typeof object.numBlocks === "string")
                    message.numBlocks = parseInt(object.numBlocks, 10);
                else if (typeof object.numBlocks === "number")
                    message.numBlocks = object.numBlocks;
                else if (typeof object.numBlocks === "object")
                    message.numBlocks = new $util.LongBits(object.numBlocks.low >>> 0, object.numBlocks.high >>> 0).toNumber(true);
            return message;
        };

        /**
         * Creates a plain object from an Extent message. Also converts values to other types if specified.
         * @function toObject
         * @memberof chromeos_update_engine.Extent
         * @static
         * @param {chromeos_update_engine.Extent} message Extent
         * @param {$protobuf.IConversionOptions} [options] Conversion options
         * @returns {Object.<string,*>} Plain object
         */
        Extent.toObject = function toObject(message, options) {
            if (!options)
                options = {};
            let object = {};
            if (options.defaults) {
                if ($util.Long) {
                    let long = new $util.Long(0, 0, true);
                    object.startBlock = options.longs === String ? long.toString() : options.longs === Number ? long.toNumber() : long;
                } else
                    object.startBlock = options.longs === String ? "0" : 0;
                if ($util.Long) {
                    let long = new $util.Long(0, 0, true);
                    object.numBlocks = options.longs === String ? long.toString() : options.longs === Number ? long.toNumber() : long;
                } else
                    object.numBlocks = options.longs === String ? "0" : 0;
            }
            if (message.startBlock != null && message.hasOwnProperty("startBlock"))
                if (typeof message.startBlock === "number")
                    object.startBlock = options.longs === String ? String(message.startBlock) : message.startBlock;
                else
                    object.startBlock = options.longs === String ? $util.Long.prototype.toString.call(message.startBlock) : options.longs === Number ? new $util.LongBits(message.startBlock.low >>> 0, message.startBlock.high >>> 0).toNumber(true) : message.startBlock;
            if (message.numBlocks != null && message.hasOwnProperty("numBlocks"))
                if (typeof message.numBlocks === "number")
                    object.numBlocks = options.longs === String ? String(message.numBlocks) : message.numBlocks;
                else
                    object.numBlocks = options.longs === String ? $util.Long.prototype.toString.call(message.numBlocks) : options.longs === Number ? new $util.LongBits(message.numBlocks.low >>> 0, message.numBlocks.high >>> 0).toNumber(true) : message.numBlocks;
            return object;
        };

        /**
         * Converts this Extent to JSON.
         * @function toJSON
         * @memberof chromeos_update_engine.Extent
         * @instance
         * @returns {Object.<string,*>} JSON object
         */
        Extent.prototype.toJSON = function toJSON() {
            return this.constructor.toObject(this, $protobuf.util.toJSONOptions);
        };

        return Extent;
    })();

    chromeos_update_engine.Signatures = (function() {

        /**
         * Properties of a Signatures.
         * @memberof chromeos_update_engine
         * @interface ISignatures
         * @property {Array.<chromeos_update_engine.Signatures.ISignature>|null} [signatures] Signatures signatures
         */

        /**
         * Constructs a new Signatures.
         * @memberof chromeos_update_engine
         * @classdesc Represents a Signatures.
         * @implements ISignatures
         * @constructor
         * @param {chromeos_update_engine.ISignatures=} [properties] Properties to set
         */
        function Signatures(properties) {
            this.signatures = [];
            if (properties)
                for (let keys = Object.keys(properties), i = 0; i < keys.length; ++i)
                    if (properties[keys[i]] != null)
                        this[keys[i]] = properties[keys[i]];
        }

        /**
         * Signatures signatures.
         * @member {Array.<chromeos_update_engine.Signatures.ISignature>} signatures
         * @memberof chromeos_update_engine.Signatures
         * @instance
         */
        Signatures.prototype.signatures = $util.emptyArray;

        /**
         * Creates a new Signatures instance using the specified properties.
         * @function create
         * @memberof chromeos_update_engine.Signatures
         * @static
         * @param {chromeos_update_engine.ISignatures=} [properties] Properties to set
         * @returns {chromeos_update_engine.Signatures} Signatures instance
         */
        Signatures.create = function create(properties) {
            return new Signatures(properties);
        };

        /**
         * Encodes the specified Signatures message. Does not implicitly {@link chromeos_update_engine.Signatures.verify|verify} messages.
         * @function encode
         * @memberof chromeos_update_engine.Signatures
         * @static
         * @param {chromeos_update_engine.ISignatures} message Signatures message or plain object to encode
         * @param {$protobuf.Writer} [writer] Writer to encode to
         * @returns {$protobuf.Writer} Writer
         */
        Signatures.encode = function encode(message, writer) {
            if (!writer)
                writer = $Writer.create();
            if (message.signatures != null && message.signatures.length)
                for (let i = 0; i < message.signatures.length; ++i)
                    $root.chromeos_update_engine.Signatures.Signature.encode(message.signatures[i], writer.uint32(/* id 1, wireType 2 =*/10).fork()).ldelim();
            return writer;
        };

        /**
         * Encodes the specified Signatures message, length delimited. Does not implicitly {@link chromeos_update_engine.Signatures.verify|verify} messages.
         * @function encodeDelimited
         * @memberof chromeos_update_engine.Signatures
         * @static
         * @param {chromeos_update_engine.ISignatures} message Signatures message or plain object to encode
         * @param {$protobuf.Writer} [writer] Writer to encode to
         * @returns {$protobuf.Writer} Writer
         */
        Signatures.encodeDelimited = function encodeDelimited(message, writer) {
            return this.encode(message, writer).ldelim();
        };

        /**
         * Decodes a Signatures message from the specified reader or buffer.
         * @function decode
         * @memberof chromeos_update_engine.Signatures
         * @static
         * @param {$protobuf.Reader|Uint8Array} reader Reader or buffer to decode from
         * @param {number} [length] Message length if known beforehand
         * @returns {chromeos_update_engine.Signatures} Signatures
         * @throws {Error} If the payload is not a reader or valid buffer
         * @throws {$protobuf.util.ProtocolError} If required fields are missing
         */
        Signatures.decode = function decode(reader, length) {
            if (!(reader instanceof $Reader))
                reader = $Reader.create(reader);
            let end = length === undefined ? reader.len : reader.pos + length, message = new $root.chromeos_update_engine.Signatures();
            while (reader.pos < end) {
                let tag = reader.uint32();
                switch (tag >>> 3) {
                case 1:
                    if (!(message.signatures && message.signatures.length))
                        message.signatures = [];
                    message.signatures.push($root.chromeos_update_engine.Signatures.Signature.decode(reader, reader.uint32()));
                    break;
                default:
                    reader.skipType(tag & 7);
                    break;
                }
            }
            return message;
        };

        /**
         * Decodes a Signatures message from the specified reader or buffer, length delimited.
         * @function decodeDelimited
         * @memberof chromeos_update_engine.Signatures
         * @static
         * @param {$protobuf.Reader|Uint8Array} reader Reader or buffer to decode from
         * @returns {chromeos_update_engine.Signatures} Signatures
         * @throws {Error} If the payload is not a reader or valid buffer
         * @throws {$protobuf.util.ProtocolError} If required fields are missing
         */
        Signatures.decodeDelimited = function decodeDelimited(reader) {
            if (!(reader instanceof $Reader))
                reader = new $Reader(reader);
            return this.decode(reader, reader.uint32());
        };

        /**
         * Verifies a Signatures message.
         * @function verify
         * @memberof chromeos_update_engine.Signatures
         * @static
         * @param {Object.<string,*>} message Plain object to verify
         * @returns {string|null} `null` if valid, otherwise the reason why it is not
         */
        Signatures.verify = function verify(message) {
            if (typeof message !== "object" || message === null)
                return "object expected";
            if (message.signatures != null && message.hasOwnProperty("signatures")) {
                if (!Array.isArray(message.signatures))
                    return "signatures: array expected";
                for (let i = 0; i < message.signatures.length; ++i) {
                    let error = $root.chromeos_update_engine.Signatures.Signature.verify(message.signatures[i]);
                    if (error)
                        return "signatures." + error;
                }
            }
            return null;
        };

        /**
         * Creates a Signatures message from a plain object. Also converts values to their respective internal types.
         * @function fromObject
         * @memberof chromeos_update_engine.Signatures
         * @static
         * @param {Object.<string,*>} object Plain object
         * @returns {chromeos_update_engine.Signatures} Signatures
         */
        Signatures.fromObject = function fromObject(object) {
            if (object instanceof $root.chromeos_update_engine.Signatures)
                return object;
            let message = new $root.chromeos_update_engine.Signatures();
            if (object.signatures) {
                if (!Array.isArray(object.signatures))
                    throw TypeError(".chromeos_update_engine.Signatures.signatures: array expected");
                message.signatures = [];
                for (let i = 0; i < object.signatures.length; ++i) {
                    if (typeof object.signatures[i] !== "object")
                        throw TypeError(".chromeos_update_engine.Signatures.signatures: object expected");
                    message.signatures[i] = $root.chromeos_update_engine.Signatures.Signature.fromObject(object.signatures[i]);
                }
            }
            return message;
        };

        /**
         * Creates a plain object from a Signatures message. Also converts values to other types if specified.
         * @function toObject
         * @memberof chromeos_update_engine.Signatures
         * @static
         * @param {chromeos_update_engine.Signatures} message Signatures
         * @param {$protobuf.IConversionOptions} [options] Conversion options
         * @returns {Object.<string,*>} Plain object
         */
        Signatures.toObject = function toObject(message, options) {
            if (!options)
                options = {};
            let object = {};
            if (options.arrays || options.defaults)
                object.signatures = [];
            if (message.signatures && message.signatures.length) {
                object.signatures = [];
                for (let j = 0; j < message.signatures.length; ++j)
                    object.signatures[j] = $root.chromeos_update_engine.Signatures.Signature.toObject(message.signatures[j], options);
            }
            return object;
        };

        /**
         * Converts this Signatures to JSON.
         * @function toJSON
         * @memberof chromeos_update_engine.Signatures
         * @instance
         * @returns {Object.<string,*>} JSON object
         */
        Signatures.prototype.toJSON = function toJSON() {
            return this.constructor.toObject(this, $protobuf.util.toJSONOptions);
        };

        Signatures.Signature = (function() {

            /**
             * Properties of a Signature.
             * @memberof chromeos_update_engine.Signatures
             * @interface ISignature
             * @property {number|null} [version] Signature version
             * @property {Uint8Array|null} [data] Signature data
             * @property {number|null} [unpaddedSignatureSize] Signature unpaddedSignatureSize
             */

            /**
             * Constructs a new Signature.
             * @memberof chromeos_update_engine.Signatures
             * @classdesc Represents a Signature.
             * @implements ISignature
             * @constructor
             * @param {chromeos_update_engine.Signatures.ISignature=} [properties] Properties to set
             */
            function Signature(properties) {
                if (properties)
                    for (let keys = Object.keys(properties), i = 0; i < keys.length; ++i)
                        if (properties[keys[i]] != null)
                            this[keys[i]] = properties[keys[i]];
            }

            /**
             * Signature version.
             * @member {number} version
             * @memberof chromeos_update_engine.Signatures.Signature
             * @instance
             */
            Signature.prototype.version = 0;

            /**
             * Signature data.
             * @member {Uint8Array} data
             * @memberof chromeos_update_engine.Signatures.Signature
             * @instance
             */
            Signature.prototype.data = $util.newBuffer([]);

            /**
             * Signature unpaddedSignatureSize.
             * @member {number} unpaddedSignatureSize
             * @memberof chromeos_update_engine.Signatures.Signature
             * @instance
             */
            Signature.prototype.unpaddedSignatureSize = 0;

            /**
             * Creates a new Signature instance using the specified properties.
             * @function create
             * @memberof chromeos_update_engine.Signatures.Signature
             * @static
             * @param {chromeos_update_engine.Signatures.ISignature=} [properties] Properties to set
             * @returns {chromeos_update_engine.Signatures.Signature} Signature instance
             */
            Signature.create = function create(properties) {
                return new Signature(properties);
            };

            /**
             * Encodes the specified Signature message. Does not implicitly {@link chromeos_update_engine.Signatures.Signature.verify|verify} messages.
             * @function encode
             * @memberof chromeos_update_engine.Signatures.Signature
             * @static
             * @param {chromeos_update_engine.Signatures.ISignature} message Signature message or plain object to encode
             * @param {$protobuf.Writer} [writer] Writer to encode to
             * @returns {$protobuf.Writer} Writer
             */
            Signature.encode = function encode(message, writer) {
                if (!writer)
                    writer = $Writer.create();
                if (message.version != null && Object.hasOwnProperty.call(message, "version"))
                    writer.uint32(/* id 1, wireType 0 =*/8).uint32(message.version);
                if (message.data != null && Object.hasOwnProperty.call(message, "data"))
                    writer.uint32(/* id 2, wireType 2 =*/18).bytes(message.data);
                if (message.unpaddedSignatureSize != null && Object.hasOwnProperty.call(message, "unpaddedSignatureSize"))
                    writer.uint32(/* id 3, wireType 5 =*/29).fixed32(message.unpaddedSignatureSize);
                return writer;
            };

            /**
             * Encodes the specified Signature message, length delimited. Does not implicitly {@link chromeos_update_engine.Signatures.Signature.verify|verify} messages.
             * @function encodeDelimited
             * @memberof chromeos_update_engine.Signatures.Signature
             * @static
             * @param {chromeos_update_engine.Signatures.ISignature} message Signature message or plain object to encode
             * @param {$protobuf.Writer} [writer] Writer to encode to
             * @returns {$protobuf.Writer} Writer
             */
            Signature.encodeDelimited = function encodeDelimited(message, writer) {
                return this.encode(message, writer).ldelim();
            };

            /**
             * Decodes a Signature message from the specified reader or buffer.
             * @function decode
             * @memberof chromeos_update_engine.Signatures.Signature
             * @static
             * @param {$protobuf.Reader|Uint8Array} reader Reader or buffer to decode from
             * @param {number} [length] Message length if known beforehand
             * @returns {chromeos_update_engine.Signatures.Signature} Signature
             * @throws {Error} If the payload is not a reader or valid buffer
             * @throws {$protobuf.util.ProtocolError} If required fields are missing
             */
            Signature.decode = function decode(reader, length) {
                if (!(reader instanceof $Reader))
                    reader = $Reader.create(reader);
                let end = length === undefined ? reader.len : reader.pos + length, message = new $root.chromeos_update_engine.Signatures.Signature();
                while (reader.pos < end) {
                    let tag = reader.uint32();
                    switch (tag >>> 3) {
                    case 1:
                        message.version = reader.uint32();
                        break;
                    case 2:
                        message.data = reader.bytes();
                        break;
                    case 3:
                        message.unpaddedSignatureSize = reader.fixed32();
                        break;
                    default:
                        reader.skipType(tag & 7);
                        break;
                    }
                }
                return message;
            };

            /**
             * Decodes a Signature message from the specified reader or buffer, length delimited.
             * @function decodeDelimited
             * @memberof chromeos_update_engine.Signatures.Signature
             * @static
             * @param {$protobuf.Reader|Uint8Array} reader Reader or buffer to decode from
             * @returns {chromeos_update_engine.Signatures.Signature} Signature
             * @throws {Error} If the payload is not a reader or valid buffer
             * @throws {$protobuf.util.ProtocolError} If required fields are missing
             */
            Signature.decodeDelimited = function decodeDelimited(reader) {
                if (!(reader instanceof $Reader))
                    reader = new $Reader(reader);
                return this.decode(reader, reader.uint32());
            };

            /**
             * Verifies a Signature message.
             * @function verify
             * @memberof chromeos_update_engine.Signatures.Signature
             * @static
             * @param {Object.<string,*>} message Plain object to verify
             * @returns {string|null} `null` if valid, otherwise the reason why it is not
             */
            Signature.verify = function verify(message) {
                if (typeof message !== "object" || message === null)
                    return "object expected";
                if (message.version != null && message.hasOwnProperty("version"))
                    if (!$util.isInteger(message.version))
                        return "version: integer expected";
                if (message.data != null && message.hasOwnProperty("data"))
                    if (!(message.data && typeof message.data.length === "number" || $util.isString(message.data)))
                        return "data: buffer expected";
                if (message.unpaddedSignatureSize != null && message.hasOwnProperty("unpaddedSignatureSize"))
                    if (!$util.isInteger(message.unpaddedSignatureSize))
                        return "unpaddedSignatureSize: integer expected";
                return null;
            };

            /**
             * Creates a Signature message from a plain object. Also converts values to their respective internal types.
             * @function fromObject
             * @memberof chromeos_update_engine.Signatures.Signature
             * @static
             * @param {Object.<string,*>} object Plain object
             * @returns {chromeos_update_engine.Signatures.Signature} Signature
             */
            Signature.fromObject = function fromObject(object) {
                if (object instanceof $root.chromeos_update_engine.Signatures.Signature)
                    return object;
                let message = new $root.chromeos_update_engine.Signatures.Signature();
                if (object.version != null)
                    message.version = object.version >>> 0;
                if (object.data != null)
                    if (typeof object.data === "string")
                        $util.base64.decode(object.data, message.data = $util.newBuffer($util.base64.length(object.data)), 0);
                    else if (object.data.length)
                        message.data = object.data;
                if (object.unpaddedSignatureSize != null)
                    message.unpaddedSignatureSize = object.unpaddedSignatureSize >>> 0;
                return message;
            };

            /**
             * Creates a plain object from a Signature message. Also converts values to other types if specified.
             * @function toObject
             * @memberof chromeos_update_engine.Signatures.Signature
             * @static
             * @param {chromeos_update_engine.Signatures.Signature} message Signature
             * @param {$protobuf.IConversionOptions} [options] Conversion options
             * @returns {Object.<string,*>} Plain object
             */
            Signature.toObject = function toObject(message, options) {
                if (!options)
                    options = {};
                let object = {};
                if (options.defaults) {
                    object.version = 0;
                    if (options.bytes === String)
                        object.data = "";
                    else {
                        object.data = [];
                        if (options.bytes !== Array)
                            object.data = $util.newBuffer(object.data);
                    }
                    object.unpaddedSignatureSize = 0;
                }
                if (message.version != null && message.hasOwnProperty("version"))
                    object.version = message.version;
                if (message.data != null && message.hasOwnProperty("data"))
                    object.data = options.bytes === String ? $util.base64.encode(message.data, 0, message.data.length) : options.bytes === Array ? Array.prototype.slice.call(message.data) : message.data;
                if (message.unpaddedSignatureSize != null && message.hasOwnProperty("unpaddedSignatureSize"))
                    object.unpaddedSignatureSize = message.unpaddedSignatureSize;
                return object;
            };

            /**
             * Converts this Signature to JSON.
             * @function toJSON
             * @memberof chromeos_update_engine.Signatures.Signature
             * @instance
             * @returns {Object.<string,*>} JSON object
             */
            Signature.prototype.toJSON = function toJSON() {
                return this.constructor.toObject(this, $protobuf.util.toJSONOptions);
            };

            return Signature;
        })();

        return Signatures;
    })();

    chromeos_update_engine.PartitionInfo = (function() {

        /**
         * Properties of a PartitionInfo.
         * @memberof chromeos_update_engine
         * @interface IPartitionInfo
         * @property {number|Long|null} [size] PartitionInfo size
         * @property {Uint8Array|null} [hash] PartitionInfo hash
         */

        /**
         * Constructs a new PartitionInfo.
         * @memberof chromeos_update_engine
         * @classdesc Represents a PartitionInfo.
         * @implements IPartitionInfo
         * @constructor
         * @param {chromeos_update_engine.IPartitionInfo=} [properties] Properties to set
         */
        function PartitionInfo(properties) {
            if (properties)
                for (let keys = Object.keys(properties), i = 0; i < keys.length; ++i)
                    if (properties[keys[i]] != null)
                        this[keys[i]] = properties[keys[i]];
        }

        /**
         * PartitionInfo size.
         * @member {number|Long} size
         * @memberof chromeos_update_engine.PartitionInfo
         * @instance
         */
        PartitionInfo.prototype.size = $util.Long ? $util.Long.fromBits(0,0,true) : 0;

        /**
         * PartitionInfo hash.
         * @member {Uint8Array} hash
         * @memberof chromeos_update_engine.PartitionInfo
         * @instance
         */
        PartitionInfo.prototype.hash = $util.newBuffer([]);

        /**
         * Creates a new PartitionInfo instance using the specified properties.
         * @function create
         * @memberof chromeos_update_engine.PartitionInfo
         * @static
         * @param {chromeos_update_engine.IPartitionInfo=} [properties] Properties to set
         * @returns {chromeos_update_engine.PartitionInfo} PartitionInfo instance
         */
        PartitionInfo.create = function create(properties) {
            return new PartitionInfo(properties);
        };

        /**
         * Encodes the specified PartitionInfo message. Does not implicitly {@link chromeos_update_engine.PartitionInfo.verify|verify} messages.
         * @function encode
         * @memberof chromeos_update_engine.PartitionInfo
         * @static
         * @param {chromeos_update_engine.IPartitionInfo} message PartitionInfo message or plain object to encode
         * @param {$protobuf.Writer} [writer] Writer to encode to
         * @returns {$protobuf.Writer} Writer
         */
        PartitionInfo.encode = function encode(message, writer) {
            if (!writer)
                writer = $Writer.create();
            if (message.size != null && Object.hasOwnProperty.call(message, "size"))
                writer.uint32(/* id 1, wireType 0 =*/8).uint64(message.size);
            if (message.hash != null && Object.hasOwnProperty.call(message, "hash"))
                writer.uint32(/* id 2, wireType 2 =*/18).bytes(message.hash);
            return writer;
        };

        /**
         * Encodes the specified PartitionInfo message, length delimited. Does not implicitly {@link chromeos_update_engine.PartitionInfo.verify|verify} messages.
         * @function encodeDelimited
         * @memberof chromeos_update_engine.PartitionInfo
         * @static
         * @param {chromeos_update_engine.IPartitionInfo} message PartitionInfo message or plain object to encode
         * @param {$protobuf.Writer} [writer] Writer to encode to
         * @returns {$protobuf.Writer} Writer
         */
        PartitionInfo.encodeDelimited = function encodeDelimited(message, writer) {
            return this.encode(message, writer).ldelim();
        };

        /**
         * Decodes a PartitionInfo message from the specified reader or buffer.
         * @function decode
         * @memberof chromeos_update_engine.PartitionInfo
         * @static
         * @param {$protobuf.Reader|Uint8Array} reader Reader or buffer to decode from
         * @param {number} [length] Message length if known beforehand
         * @returns {chromeos_update_engine.PartitionInfo} PartitionInfo
         * @throws {Error} If the payload is not a reader or valid buffer
         * @throws {$protobuf.util.ProtocolError} If required fields are missing
         */
        PartitionInfo.decode = function decode(reader, length) {
            if (!(reader instanceof $Reader))
                reader = $Reader.create(reader);
            let end = length === undefined ? reader.len : reader.pos + length, message = new $root.chromeos_update_engine.PartitionInfo();
            while (reader.pos < end) {
                let tag = reader.uint32();
                switch (tag >>> 3) {
                case 1:
                    message.size = reader.uint64();
                    break;
                case 2:
                    message.hash = reader.bytes();
                    break;
                default:
                    reader.skipType(tag & 7);
                    break;
                }
            }
            return message;
        };

        /**
         * Decodes a PartitionInfo message from the specified reader or buffer, length delimited.
         * @function decodeDelimited
         * @memberof chromeos_update_engine.PartitionInfo
         * @static
         * @param {$protobuf.Reader|Uint8Array} reader Reader or buffer to decode from
         * @returns {chromeos_update_engine.PartitionInfo} PartitionInfo
         * @throws {Error} If the payload is not a reader or valid buffer
         * @throws {$protobuf.util.ProtocolError} If required fields are missing
         */
        PartitionInfo.decodeDelimited = function decodeDelimited(reader) {
            if (!(reader instanceof $Reader))
                reader = new $Reader(reader);
            return this.decode(reader, reader.uint32());
        };

        /**
         * Verifies a PartitionInfo message.
         * @function verify
         * @memberof chromeos_update_engine.PartitionInfo
         * @static
         * @param {Object.<string,*>} message Plain object to verify
         * @returns {string|null} `null` if valid, otherwise the reason why it is not
         */
        PartitionInfo.verify = function verify(message) {
            if (typeof message !== "object" || message === null)
                return "object expected";
            if (message.size != null && message.hasOwnProperty("size"))
                if (!$util.isInteger(message.size) && !(message.size && $util.isInteger(message.size.low) && $util.isInteger(message.size.high)))
                    return "size: integer|Long expected";
            if (message.hash != null && message.hasOwnProperty("hash"))
                if (!(message.hash && typeof message.hash.length === "number" || $util.isString(message.hash)))
                    return "hash: buffer expected";
            return null;
        };

        /**
         * Creates a PartitionInfo message from a plain object. Also converts values to their respective internal types.
         * @function fromObject
         * @memberof chromeos_update_engine.PartitionInfo
         * @static
         * @param {Object.<string,*>} object Plain object
         * @returns {chromeos_update_engine.PartitionInfo} PartitionInfo
         */
        PartitionInfo.fromObject = function fromObject(object) {
            if (object instanceof $root.chromeos_update_engine.PartitionInfo)
                return object;
            let message = new $root.chromeos_update_engine.PartitionInfo();
            if (object.size != null)
                if ($util.Long)
                    (message.size = $util.Long.fromValue(object.size)).unsigned = true;
                else if (typeof object.size === "string")
                    message.size = parseInt(object.size, 10);
                else if (typeof object.size === "number")
                    message.size = object.size;
                else if (typeof object.size === "object")
                    message.size = new $util.LongBits(object.size.low >>> 0, object.size.high >>> 0).toNumber(true);
            if (object.hash != null)
                if (typeof object.hash === "string")
                    $util.base64.decode(object.hash, message.hash = $util.newBuffer($util.base64.length(object.hash)), 0);
                else if (object.hash.length)
                    message.hash = object.hash;
            return message;
        };

        /**
         * Creates a plain object from a PartitionInfo message. Also converts values to other types if specified.
         * @function toObject
         * @memberof chromeos_update_engine.PartitionInfo
         * @static
         * @param {chromeos_update_engine.PartitionInfo} message PartitionInfo
         * @param {$protobuf.IConversionOptions} [options] Conversion options
         * @returns {Object.<string,*>} Plain object
         */
        PartitionInfo.toObject = function toObject(message, options) {
            if (!options)
                options = {};
            let object = {};
            if (options.defaults) {
                if ($util.Long) {
                    let long = new $util.Long(0, 0, true);
                    object.size = options.longs === String ? long.toString() : options.longs === Number ? long.toNumber() : long;
                } else
                    object.size = options.longs === String ? "0" : 0;
                if (options.bytes === String)
                    object.hash = "";
                else {
                    object.hash = [];
                    if (options.bytes !== Array)
                        object.hash = $util.newBuffer(object.hash);
                }
            }
            if (message.size != null && message.hasOwnProperty("size"))
                if (typeof message.size === "number")
                    object.size = options.longs === String ? String(message.size) : message.size;
                else
                    object.size = options.longs === String ? $util.Long.prototype.toString.call(message.size) : options.longs === Number ? new $util.LongBits(message.size.low >>> 0, message.size.high >>> 0).toNumber(true) : message.size;
            if (message.hash != null && message.hasOwnProperty("hash"))
                object.hash = options.bytes === String ? $util.base64.encode(message.hash, 0, message.hash.length) : options.bytes === Array ? Array.prototype.slice.call(message.hash) : message.hash;
            return object;
        };

        /**
         * Converts this PartitionInfo to JSON.
         * @function toJSON
         * @memberof chromeos_update_engine.PartitionInfo
         * @instance
         * @returns {Object.<string,*>} JSON object
         */
        PartitionInfo.prototype.toJSON = function toJSON() {
            return this.constructor.toObject(this, $protobuf.util.toJSONOptions);
        };

        return PartitionInfo;
    })();

    chromeos_update_engine.ImageInfo = (function() {

        /**
         * Properties of an ImageInfo.
         * @memberof chromeos_update_engine
         * @interface IImageInfo
         * @property {string|null} [board] ImageInfo board
         * @property {string|null} [key] ImageInfo key
         * @property {string|null} [channel] ImageInfo channel
         * @property {string|null} [version] ImageInfo version
         * @property {string|null} [buildChannel] ImageInfo buildChannel
         * @property {string|null} [buildVersion] ImageInfo buildVersion
         */

        /**
         * Constructs a new ImageInfo.
         * @memberof chromeos_update_engine
         * @classdesc Represents an ImageInfo.
         * @implements IImageInfo
         * @constructor
         * @param {chromeos_update_engine.IImageInfo=} [properties] Properties to set
         */
        function ImageInfo(properties) {
            if (properties)
                for (let keys = Object.keys(properties), i = 0; i < keys.length; ++i)
                    if (properties[keys[i]] != null)
                        this[keys[i]] = properties[keys[i]];
        }

        /**
         * ImageInfo board.
         * @member {string} board
         * @memberof chromeos_update_engine.ImageInfo
         * @instance
         */
        ImageInfo.prototype.board = "";

        /**
         * ImageInfo key.
         * @member {string} key
         * @memberof chromeos_update_engine.ImageInfo
         * @instance
         */
        ImageInfo.prototype.key = "";

        /**
         * ImageInfo channel.
         * @member {string} channel
         * @memberof chromeos_update_engine.ImageInfo
         * @instance
         */
        ImageInfo.prototype.channel = "";

        /**
         * ImageInfo version.
         * @member {string} version
         * @memberof chromeos_update_engine.ImageInfo
         * @instance
         */
        ImageInfo.prototype.version = "";

        /**
         * ImageInfo buildChannel.
         * @member {string} buildChannel
         * @memberof chromeos_update_engine.ImageInfo
         * @instance
         */
        ImageInfo.prototype.buildChannel = "";

        /**
         * ImageInfo buildVersion.
         * @member {string} buildVersion
         * @memberof chromeos_update_engine.ImageInfo
         * @instance
         */
        ImageInfo.prototype.buildVersion = "";

        /**
         * Creates a new ImageInfo instance using the specified properties.
         * @function create
         * @memberof chromeos_update_engine.ImageInfo
         * @static
         * @param {chromeos_update_engine.IImageInfo=} [properties] Properties to set
         * @returns {chromeos_update_engine.ImageInfo} ImageInfo instance
         */
        ImageInfo.create = function create(properties) {
            return new ImageInfo(properties);
        };

        /**
         * Encodes the specified ImageInfo message. Does not implicitly {@link chromeos_update_engine.ImageInfo.verify|verify} messages.
         * @function encode
         * @memberof chromeos_update_engine.ImageInfo
         * @static
         * @param {chromeos_update_engine.IImageInfo} message ImageInfo message or plain object to encode
         * @param {$protobuf.Writer} [writer] Writer to encode to
         * @returns {$protobuf.Writer} Writer
         */
        ImageInfo.encode = function encode(message, writer) {
            if (!writer)
                writer = $Writer.create();
            if (message.board != null && Object.hasOwnProperty.call(message, "board"))
                writer.uint32(/* id 1, wireType 2 =*/10).string(message.board);
            if (message.key != null && Object.hasOwnProperty.call(message, "key"))
                writer.uint32(/* id 2, wireType 2 =*/18).string(message.key);
            if (message.channel != null && Object.hasOwnProperty.call(message, "channel"))
                writer.uint32(/* id 3, wireType 2 =*/26).string(message.channel);
            if (message.version != null && Object.hasOwnProperty.call(message, "version"))
                writer.uint32(/* id 4, wireType 2 =*/34).string(message.version);
            if (message.buildChannel != null && Object.hasOwnProperty.call(message, "buildChannel"))
                writer.uint32(/* id 5, wireType 2 =*/42).string(message.buildChannel);
            if (message.buildVersion != null && Object.hasOwnProperty.call(message, "buildVersion"))
                writer.uint32(/* id 6, wireType 2 =*/50).string(message.buildVersion);
            return writer;
        };

        /**
         * Encodes the specified ImageInfo message, length delimited. Does not implicitly {@link chromeos_update_engine.ImageInfo.verify|verify} messages.
         * @function encodeDelimited
         * @memberof chromeos_update_engine.ImageInfo
         * @static
         * @param {chromeos_update_engine.IImageInfo} message ImageInfo message or plain object to encode
         * @param {$protobuf.Writer} [writer] Writer to encode to
         * @returns {$protobuf.Writer} Writer
         */
        ImageInfo.encodeDelimited = function encodeDelimited(message, writer) {
            return this.encode(message, writer).ldelim();
        };

        /**
         * Decodes an ImageInfo message from the specified reader or buffer.
         * @function decode
         * @memberof chromeos_update_engine.ImageInfo
         * @static
         * @param {$protobuf.Reader|Uint8Array} reader Reader or buffer to decode from
         * @param {number} [length] Message length if known beforehand
         * @returns {chromeos_update_engine.ImageInfo} ImageInfo
         * @throws {Error} If the payload is not a reader or valid buffer
         * @throws {$protobuf.util.ProtocolError} If required fields are missing
         */
        ImageInfo.decode = function decode(reader, length) {
            if (!(reader instanceof $Reader))
                reader = $Reader.create(reader);
            let end = length === undefined ? reader.len : reader.pos + length, message = new $root.chromeos_update_engine.ImageInfo();
            while (reader.pos < end) {
                let tag = reader.uint32();
                switch (tag >>> 3) {
                case 1:
                    message.board = reader.string();
                    break;
                case 2:
                    message.key = reader.string();
                    break;
                case 3:
                    message.channel = reader.string();
                    break;
                case 4:
                    message.version = reader.string();
                    break;
                case 5:
                    message.buildChannel = reader.string();
                    break;
                case 6:
                    message.buildVersion = reader.string();
                    break;
                default:
                    reader.skipType(tag & 7);
                    break;
                }
            }
            return message;
        };

        /**
         * Decodes an ImageInfo message from the specified reader or buffer, length delimited.
         * @function decodeDelimited
         * @memberof chromeos_update_engine.ImageInfo
         * @static
         * @param {$protobuf.Reader|Uint8Array} reader Reader or buffer to decode from
         * @returns {chromeos_update_engine.ImageInfo} ImageInfo
         * @throws {Error} If the payload is not a reader or valid buffer
         * @throws {$protobuf.util.ProtocolError} If required fields are missing
         */
        ImageInfo.decodeDelimited = function decodeDelimited(reader) {
            if (!(reader instanceof $Reader))
                reader = new $Reader(reader);
            return this.decode(reader, reader.uint32());
        };

        /**
         * Verifies an ImageInfo message.
         * @function verify
         * @memberof chromeos_update_engine.ImageInfo
         * @static
         * @param {Object.<string,*>} message Plain object to verify
         * @returns {string|null} `null` if valid, otherwise the reason why it is not
         */
        ImageInfo.verify = function verify(message) {
            if (typeof message !== "object" || message === null)
                return "object expected";
            if (message.board != null && message.hasOwnProperty("board"))
                if (!$util.isString(message.board))
                    return "board: string expected";
            if (message.key != null && message.hasOwnProperty("key"))
                if (!$util.isString(message.key))
                    return "key: string expected";
            if (message.channel != null && message.hasOwnProperty("channel"))
                if (!$util.isString(message.channel))
                    return "channel: string expected";
            if (message.version != null && message.hasOwnProperty("version"))
                if (!$util.isString(message.version))
                    return "version: string expected";
            if (message.buildChannel != null && message.hasOwnProperty("buildChannel"))
                if (!$util.isString(message.buildChannel))
                    return "buildChannel: string expected";
            if (message.buildVersion != null && message.hasOwnProperty("buildVersion"))
                if (!$util.isString(message.buildVersion))
                    return "buildVersion: string expected";
            return null;
        };

        /**
         * Creates an ImageInfo message from a plain object. Also converts values to their respective internal types.
         * @function fromObject
         * @memberof chromeos_update_engine.ImageInfo
         * @static
         * @param {Object.<string,*>} object Plain object
         * @returns {chromeos_update_engine.ImageInfo} ImageInfo
         */
        ImageInfo.fromObject = function fromObject(object) {
            if (object instanceof $root.chromeos_update_engine.ImageInfo)
                return object;
            let message = new $root.chromeos_update_engine.ImageInfo();
            if (object.board != null)
                message.board = String(object.board);
            if (object.key != null)
                message.key = String(object.key);
            if (object.channel != null)
                message.channel = String(object.channel);
            if (object.version != null)
                message.version = String(object.version);
            if (object.buildChannel != null)
                message.buildChannel = String(object.buildChannel);
            if (object.buildVersion != null)
                message.buildVersion = String(object.buildVersion);
            return message;
        };

        /**
         * Creates a plain object from an ImageInfo message. Also converts values to other types if specified.
         * @function toObject
         * @memberof chromeos_update_engine.ImageInfo
         * @static
         * @param {chromeos_update_engine.ImageInfo} message ImageInfo
         * @param {$protobuf.IConversionOptions} [options] Conversion options
         * @returns {Object.<string,*>} Plain object
         */
        ImageInfo.toObject = function toObject(message, options) {
            if (!options)
                options = {};
            let object = {};
            if (options.defaults) {
                object.board = "";
                object.key = "";
                object.channel = "";
                object.version = "";
                object.buildChannel = "";
                object.buildVersion = "";
            }
            if (message.board != null && message.hasOwnProperty("board"))
                object.board = message.board;
            if (message.key != null && message.hasOwnProperty("key"))
                object.key = message.key;
            if (message.channel != null && message.hasOwnProperty("channel"))
                object.channel = message.channel;
            if (message.version != null && message.hasOwnProperty("version"))
                object.version = message.version;
            if (message.buildChannel != null && message.hasOwnProperty("buildChannel"))
                object.buildChannel = message.buildChannel;
            if (message.buildVersion != null && message.hasOwnProperty("buildVersion"))
                object.buildVersion = message.buildVersion;
            return object;
        };

        /**
         * Converts this ImageInfo to JSON.
         * @function toJSON
         * @memberof chromeos_update_engine.ImageInfo
         * @instance
         * @returns {Object.<string,*>} JSON object
         */
        ImageInfo.prototype.toJSON = function toJSON() {
            return this.constructor.toObject(this, $protobuf.util.toJSONOptions);
        };

        return ImageInfo;
    })();

    chromeos_update_engine.InstallOperation = (function() {

        /**
         * Properties of an InstallOperation.
         * @memberof chromeos_update_engine
         * @interface IInstallOperation
         * @property {chromeos_update_engine.InstallOperation.Type} type InstallOperation type
         * @property {number|Long|null} [dataOffset] InstallOperation dataOffset
         * @property {number|Long|null} [dataLength] InstallOperation dataLength
         * @property {Array.<chromeos_update_engine.IExtent>|null} [srcExtents] InstallOperation srcExtents
         * @property {number|Long|null} [srcLength] InstallOperation srcLength
         * @property {Array.<chromeos_update_engine.IExtent>|null} [dstExtents] InstallOperation dstExtents
         * @property {number|Long|null} [dstLength] InstallOperation dstLength
         * @property {Uint8Array|null} [dataSha256Hash] InstallOperation dataSha256Hash
         * @property {Uint8Array|null} [srcSha256Hash] InstallOperation srcSha256Hash
         * @property {Object.<string,number>|null} [xorMap] InstallOperation xorMap
         */

        /**
         * Constructs a new InstallOperation.
         * @memberof chromeos_update_engine
         * @classdesc Represents an InstallOperation.
         * @implements IInstallOperation
         * @constructor
         * @param {chromeos_update_engine.IInstallOperation=} [properties] Properties to set
         */
        function InstallOperation(properties) {
            this.srcExtents = [];
            this.dstExtents = [];
            this.xorMap = {};
            if (properties)
                for (let keys = Object.keys(properties), i = 0; i < keys.length; ++i)
                    if (properties[keys[i]] != null)
                        this[keys[i]] = properties[keys[i]];
        }

        /**
         * InstallOperation type.
         * @member {chromeos_update_engine.InstallOperation.Type} type
         * @memberof chromeos_update_engine.InstallOperation
         * @instance
         */
        InstallOperation.prototype.type = 0;

        /**
         * InstallOperation dataOffset.
         * @member {number|Long} dataOffset
         * @memberof chromeos_update_engine.InstallOperation
         * @instance
         */
        InstallOperation.prototype.dataOffset = $util.Long ? $util.Long.fromBits(0,0,true) : 0;

        /**
         * InstallOperation dataLength.
         * @member {number|Long} dataLength
         * @memberof chromeos_update_engine.InstallOperation
         * @instance
         */
        InstallOperation.prototype.dataLength = $util.Long ? $util.Long.fromBits(0,0,true) : 0;

        /**
         * InstallOperation srcExtents.
         * @member {Array.<chromeos_update_engine.IExtent>} srcExtents
         * @memberof chromeos_update_engine.InstallOperation
         * @instance
         */
        InstallOperation.prototype.srcExtents = $util.emptyArray;

        /**
         * InstallOperation srcLength.
         * @member {number|Long} srcLength
         * @memberof chromeos_update_engine.InstallOperation
         * @instance
         */
        InstallOperation.prototype.srcLength = $util.Long ? $util.Long.fromBits(0,0,true) : 0;

        /**
         * InstallOperation dstExtents.
         * @member {Array.<chromeos_update_engine.IExtent>} dstExtents
         * @memberof chromeos_update_engine.InstallOperation
         * @instance
         */
        InstallOperation.prototype.dstExtents = $util.emptyArray;

        /**
         * InstallOperation dstLength.
         * @member {number|Long} dstLength
         * @memberof chromeos_update_engine.InstallOperation
         * @instance
         */
        InstallOperation.prototype.dstLength = $util.Long ? $util.Long.fromBits(0,0,true) : 0;

        /**
         * InstallOperation dataSha256Hash.
         * @member {Uint8Array} dataSha256Hash
         * @memberof chromeos_update_engine.InstallOperation
         * @instance
         */
        InstallOperation.prototype.dataSha256Hash = $util.newBuffer([]);

        /**
         * InstallOperation srcSha256Hash.
         * @member {Uint8Array} srcSha256Hash
         * @memberof chromeos_update_engine.InstallOperation
         * @instance
         */
        InstallOperation.prototype.srcSha256Hash = $util.newBuffer([]);

        /**
         * InstallOperation xorMap.
         * @member {Object.<string,number>} xorMap
         * @memberof chromeos_update_engine.InstallOperation
         * @instance
         */
        InstallOperation.prototype.xorMap = $util.emptyObject;

        /**
         * Creates a new InstallOperation instance using the specified properties.
         * @function create
         * @memberof chromeos_update_engine.InstallOperation
         * @static
         * @param {chromeos_update_engine.IInstallOperation=} [properties] Properties to set
         * @returns {chromeos_update_engine.InstallOperation} InstallOperation instance
         */
        InstallOperation.create = function create(properties) {
            return new InstallOperation(properties);
        };

        /**
         * Encodes the specified InstallOperation message. Does not implicitly {@link chromeos_update_engine.InstallOperation.verify|verify} messages.
         * @function encode
         * @memberof chromeos_update_engine.InstallOperation
         * @static
         * @param {chromeos_update_engine.IInstallOperation} message InstallOperation message or plain object to encode
         * @param {$protobuf.Writer} [writer] Writer to encode to
         * @returns {$protobuf.Writer} Writer
         */
        InstallOperation.encode = function encode(message, writer) {
            if (!writer)
                writer = $Writer.create();
            writer.uint32(/* id 1, wireType 0 =*/8).int32(message.type);
            if (message.dataOffset != null && Object.hasOwnProperty.call(message, "dataOffset"))
                writer.uint32(/* id 2, wireType 0 =*/16).uint64(message.dataOffset);
            if (message.dataLength != null && Object.hasOwnProperty.call(message, "dataLength"))
                writer.uint32(/* id 3, wireType 0 =*/24).uint64(message.dataLength);
            if (message.srcExtents != null && message.srcExtents.length)
                for (let i = 0; i < message.srcExtents.length; ++i)
                    $root.chromeos_update_engine.Extent.encode(message.srcExtents[i], writer.uint32(/* id 4, wireType 2 =*/34).fork()).ldelim();
            if (message.srcLength != null && Object.hasOwnProperty.call(message, "srcLength"))
                writer.uint32(/* id 5, wireType 0 =*/40).uint64(message.srcLength);
            if (message.dstExtents != null && message.dstExtents.length)
                for (let i = 0; i < message.dstExtents.length; ++i)
                    $root.chromeos_update_engine.Extent.encode(message.dstExtents[i], writer.uint32(/* id 6, wireType 2 =*/50).fork()).ldelim();
            if (message.dstLength != null && Object.hasOwnProperty.call(message, "dstLength"))
                writer.uint32(/* id 7, wireType 0 =*/56).uint64(message.dstLength);
            if (message.dataSha256Hash != null && Object.hasOwnProperty.call(message, "dataSha256Hash"))
                writer.uint32(/* id 8, wireType 2 =*/66).bytes(message.dataSha256Hash);
            if (message.srcSha256Hash != null && Object.hasOwnProperty.call(message, "srcSha256Hash"))
                writer.uint32(/* id 9, wireType 2 =*/74).bytes(message.srcSha256Hash);
            if (message.xorMap != null && Object.hasOwnProperty.call(message, "xorMap"))
                for (let keys = Object.keys(message.xorMap), i = 0; i < keys.length; ++i)
                    writer.uint32(/* id 10, wireType 2 =*/82).fork().uint32(/* id 1, wireType 0 =*/8).uint32(keys[i]).uint32(/* id 2, wireType 0 =*/16).uint32(message.xorMap[keys[i]]).ldelim();
            return writer;
        };

        /**
         * Encodes the specified InstallOperation message, length delimited. Does not implicitly {@link chromeos_update_engine.InstallOperation.verify|verify} messages.
         * @function encodeDelimited
         * @memberof chromeos_update_engine.InstallOperation
         * @static
         * @param {chromeos_update_engine.IInstallOperation} message InstallOperation message or plain object to encode
         * @param {$protobuf.Writer} [writer] Writer to encode to
         * @returns {$protobuf.Writer} Writer
         */
        InstallOperation.encodeDelimited = function encodeDelimited(message, writer) {
            return this.encode(message, writer).ldelim();
        };

        /**
         * Decodes an InstallOperation message from the specified reader or buffer.
         * @function decode
         * @memberof chromeos_update_engine.InstallOperation
         * @static
         * @param {$protobuf.Reader|Uint8Array} reader Reader or buffer to decode from
         * @param {number} [length] Message length if known beforehand
         * @returns {chromeos_update_engine.InstallOperation} InstallOperation
         * @throws {Error} If the payload is not a reader or valid buffer
         * @throws {$protobuf.util.ProtocolError} If required fields are missing
         */
        InstallOperation.decode = function decode(reader, length) {
            if (!(reader instanceof $Reader))
                reader = $Reader.create(reader);
            let end = length === undefined ? reader.len : reader.pos + length, message = new $root.chromeos_update_engine.InstallOperation(), key, value;
            while (reader.pos < end) {
                let tag = reader.uint32();
                switch (tag >>> 3) {
                case 1:
                    message.type = reader.int32();
                    break;
                case 2:
                    message.dataOffset = reader.uint64();
                    break;
                case 3:
                    message.dataLength = reader.uint64();
                    break;
                case 4:
                    if (!(message.srcExtents && message.srcExtents.length))
                        message.srcExtents = [];
                    message.srcExtents.push($root.chromeos_update_engine.Extent.decode(reader, reader.uint32()));
                    break;
                case 5:
                    message.srcLength = reader.uint64();
                    break;
                case 6:
                    if (!(message.dstExtents && message.dstExtents.length))
                        message.dstExtents = [];
                    message.dstExtents.push($root.chromeos_update_engine.Extent.decode(reader, reader.uint32()));
                    break;
                case 7:
                    message.dstLength = reader.uint64();
                    break;
                case 8:
                    message.dataSha256Hash = reader.bytes();
                    break;
                case 9:
                    message.srcSha256Hash = reader.bytes();
                    break;
                case 10:
                    if (message.xorMap === $util.emptyObject)
                        message.xorMap = {};
                    let end2 = reader.uint32() + reader.pos;
                    key = 0;
                    value = 0;
                    while (reader.pos < end2) {
                        let tag2 = reader.uint32();
                        switch (tag2 >>> 3) {
                        case 1:
                            key = reader.uint32();
                            break;
                        case 2:
                            value = reader.uint32();
                            break;
                        default:
                            reader.skipType(tag2 & 7);
                            break;
                        }
                    }
                    message.xorMap[key] = value;
                    break;
                default:
                    reader.skipType(tag & 7);
                    break;
                }
            }
            if (!message.hasOwnProperty("type"))
                throw $util.ProtocolError("missing required 'type'", { instance: message });
            return message;
        };

        /**
         * Decodes an InstallOperation message from the specified reader or buffer, length delimited.
         * @function decodeDelimited
         * @memberof chromeos_update_engine.InstallOperation
         * @static
         * @param {$protobuf.Reader|Uint8Array} reader Reader or buffer to decode from
         * @returns {chromeos_update_engine.InstallOperation} InstallOperation
         * @throws {Error} If the payload is not a reader or valid buffer
         * @throws {$protobuf.util.ProtocolError} If required fields are missing
         */
        InstallOperation.decodeDelimited = function decodeDelimited(reader) {
            if (!(reader instanceof $Reader))
                reader = new $Reader(reader);
            return this.decode(reader, reader.uint32());
        };

        /**
         * Verifies an InstallOperation message.
         * @function verify
         * @memberof chromeos_update_engine.InstallOperation
         * @static
         * @param {Object.<string,*>} message Plain object to verify
         * @returns {string|null} `null` if valid, otherwise the reason why it is not
         */
        InstallOperation.verify = function verify(message) {
            if (typeof message !== "object" || message === null)
                return "object expected";
            switch (message.type) {
            default:
                return "type: enum value expected";
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 8:
            case 6:
            case 7:
            case 10:
            case 9:
                break;
            }
            if (message.dataOffset != null && message.hasOwnProperty("dataOffset"))
                if (!$util.isInteger(message.dataOffset) && !(message.dataOffset && $util.isInteger(message.dataOffset.low) && $util.isInteger(message.dataOffset.high)))
                    return "dataOffset: integer|Long expected";
            if (message.dataLength != null && message.hasOwnProperty("dataLength"))
                if (!$util.isInteger(message.dataLength) && !(message.dataLength && $util.isInteger(message.dataLength.low) && $util.isInteger(message.dataLength.high)))
                    return "dataLength: integer|Long expected";
            if (message.srcExtents != null && message.hasOwnProperty("srcExtents")) {
                if (!Array.isArray(message.srcExtents))
                    return "srcExtents: array expected";
                for (let i = 0; i < message.srcExtents.length; ++i) {
                    let error = $root.chromeos_update_engine.Extent.verify(message.srcExtents[i]);
                    if (error)
                        return "srcExtents." + error;
                }
            }
            if (message.srcLength != null && message.hasOwnProperty("srcLength"))
                if (!$util.isInteger(message.srcLength) && !(message.srcLength && $util.isInteger(message.srcLength.low) && $util.isInteger(message.srcLength.high)))
                    return "srcLength: integer|Long expected";
            if (message.dstExtents != null && message.hasOwnProperty("dstExtents")) {
                if (!Array.isArray(message.dstExtents))
                    return "dstExtents: array expected";
                for (let i = 0; i < message.dstExtents.length; ++i) {
                    let error = $root.chromeos_update_engine.Extent.verify(message.dstExtents[i]);
                    if (error)
                        return "dstExtents." + error;
                }
            }
            if (message.dstLength != null && message.hasOwnProperty("dstLength"))
                if (!$util.isInteger(message.dstLength) && !(message.dstLength && $util.isInteger(message.dstLength.low) && $util.isInteger(message.dstLength.high)))
                    return "dstLength: integer|Long expected";
            if (message.dataSha256Hash != null && message.hasOwnProperty("dataSha256Hash"))
                if (!(message.dataSha256Hash && typeof message.dataSha256Hash.length === "number" || $util.isString(message.dataSha256Hash)))
                    return "dataSha256Hash: buffer expected";
            if (message.srcSha256Hash != null && message.hasOwnProperty("srcSha256Hash"))
                if (!(message.srcSha256Hash && typeof message.srcSha256Hash.length === "number" || $util.isString(message.srcSha256Hash)))
                    return "srcSha256Hash: buffer expected";
            if (message.xorMap != null && message.hasOwnProperty("xorMap")) {
                if (!$util.isObject(message.xorMap))
                    return "xorMap: object expected";
                let key = Object.keys(message.xorMap);
                for (let i = 0; i < key.length; ++i) {
                    if (!$util.key32Re.test(key[i]))
                        return "xorMap: integer key{k:uint32} expected";
                    if (!$util.isInteger(message.xorMap[key[i]]))
                        return "xorMap: integer{k:uint32} expected";
                }
            }
            return null;
        };

        /**
         * Creates an InstallOperation message from a plain object. Also converts values to their respective internal types.
         * @function fromObject
         * @memberof chromeos_update_engine.InstallOperation
         * @static
         * @param {Object.<string,*>} object Plain object
         * @returns {chromeos_update_engine.InstallOperation} InstallOperation
         */
        InstallOperation.fromObject = function fromObject(object) {
            if (object instanceof $root.chromeos_update_engine.InstallOperation)
                return object;
            let message = new $root.chromeos_update_engine.InstallOperation();
            switch (object.type) {
            case "REPLACE":
            case 0:
                message.type = 0;
                break;
            case "REPLACE_BZ":
            case 1:
                message.type = 1;
                break;
            case "MOVE":
            case 2:
                message.type = 2;
                break;
            case "BSDIFF":
            case 3:
                message.type = 3;
                break;
            case "SOURCE_COPY":
            case 4:
                message.type = 4;
                break;
            case "SOURCE_BSDIFF":
            case 5:
                message.type = 5;
                break;
            case "REPLACE_XZ":
            case 8:
                message.type = 8;
                break;
            case "ZERO":
            case 6:
                message.type = 6;
                break;
            case "DISCARD":
            case 7:
                message.type = 7;
                break;
            case "BROTLI_BSDIFF":
            case 10:
                message.type = 10;
                break;
            case "PUFFDIFF":
            case 9:
                message.type = 9;
                break;
            }
            if (object.dataOffset != null)
                if ($util.Long)
                    (message.dataOffset = $util.Long.fromValue(object.dataOffset)).unsigned = true;
                else if (typeof object.dataOffset === "string")
                    message.dataOffset = parseInt(object.dataOffset, 10);
                else if (typeof object.dataOffset === "number")
                    message.dataOffset = object.dataOffset;
                else if (typeof object.dataOffset === "object")
                    message.dataOffset = new $util.LongBits(object.dataOffset.low >>> 0, object.dataOffset.high >>> 0).toNumber(true);
            if (object.dataLength != null)
                if ($util.Long)
                    (message.dataLength = $util.Long.fromValue(object.dataLength)).unsigned = true;
                else if (typeof object.dataLength === "string")
                    message.dataLength = parseInt(object.dataLength, 10);
                else if (typeof object.dataLength === "number")
                    message.dataLength = object.dataLength;
                else if (typeof object.dataLength === "object")
                    message.dataLength = new $util.LongBits(object.dataLength.low >>> 0, object.dataLength.high >>> 0).toNumber(true);
            if (object.srcExtents) {
                if (!Array.isArray(object.srcExtents))
                    throw TypeError(".chromeos_update_engine.InstallOperation.srcExtents: array expected");
                message.srcExtents = [];
                for (let i = 0; i < object.srcExtents.length; ++i) {
                    if (typeof object.srcExtents[i] !== "object")
                        throw TypeError(".chromeos_update_engine.InstallOperation.srcExtents: object expected");
                    message.srcExtents[i] = $root.chromeos_update_engine.Extent.fromObject(object.srcExtents[i]);
                }
            }
            if (object.srcLength != null)
                if ($util.Long)
                    (message.srcLength = $util.Long.fromValue(object.srcLength)).unsigned = true;
                else if (typeof object.srcLength === "string")
                    message.srcLength = parseInt(object.srcLength, 10);
                else if (typeof object.srcLength === "number")
                    message.srcLength = object.srcLength;
                else if (typeof object.srcLength === "object")
                    message.srcLength = new $util.LongBits(object.srcLength.low >>> 0, object.srcLength.high >>> 0).toNumber(true);
            if (object.dstExtents) {
                if (!Array.isArray(object.dstExtents))
                    throw TypeError(".chromeos_update_engine.InstallOperation.dstExtents: array expected");
                message.dstExtents = [];
                for (let i = 0; i < object.dstExtents.length; ++i) {
                    if (typeof object.dstExtents[i] !== "object")
                        throw TypeError(".chromeos_update_engine.InstallOperation.dstExtents: object expected");
                    message.dstExtents[i] = $root.chromeos_update_engine.Extent.fromObject(object.dstExtents[i]);
                }
            }
            if (object.dstLength != null)
                if ($util.Long)
                    (message.dstLength = $util.Long.fromValue(object.dstLength)).unsigned = true;
                else if (typeof object.dstLength === "string")
                    message.dstLength = parseInt(object.dstLength, 10);
                else if (typeof object.dstLength === "number")
                    message.dstLength = object.dstLength;
                else if (typeof object.dstLength === "object")
                    message.dstLength = new $util.LongBits(object.dstLength.low >>> 0, object.dstLength.high >>> 0).toNumber(true);
            if (object.dataSha256Hash != null)
                if (typeof object.dataSha256Hash === "string")
                    $util.base64.decode(object.dataSha256Hash, message.dataSha256Hash = $util.newBuffer($util.base64.length(object.dataSha256Hash)), 0);
                else if (object.dataSha256Hash.length)
                    message.dataSha256Hash = object.dataSha256Hash;
            if (object.srcSha256Hash != null)
                if (typeof object.srcSha256Hash === "string")
                    $util.base64.decode(object.srcSha256Hash, message.srcSha256Hash = $util.newBuffer($util.base64.length(object.srcSha256Hash)), 0);
                else if (object.srcSha256Hash.length)
                    message.srcSha256Hash = object.srcSha256Hash;
            if (object.xorMap) {
                if (typeof object.xorMap !== "object")
                    throw TypeError(".chromeos_update_engine.InstallOperation.xorMap: object expected");
                message.xorMap = {};
                for (let keys = Object.keys(object.xorMap), i = 0; i < keys.length; ++i)
                    message.xorMap[keys[i]] = object.xorMap[keys[i]] >>> 0;
            }
            return message;
        };

        /**
         * Creates a plain object from an InstallOperation message. Also converts values to other types if specified.
         * @function toObject
         * @memberof chromeos_update_engine.InstallOperation
         * @static
         * @param {chromeos_update_engine.InstallOperation} message InstallOperation
         * @param {$protobuf.IConversionOptions} [options] Conversion options
         * @returns {Object.<string,*>} Plain object
         */
        InstallOperation.toObject = function toObject(message, options) {
            if (!options)
                options = {};
            let object = {};
            if (options.arrays || options.defaults) {
                object.srcExtents = [];
                object.dstExtents = [];
            }
            if (options.objects || options.defaults)
                object.xorMap = {};
            if (options.defaults) {
                object.type = options.enums === String ? "REPLACE" : 0;
                if ($util.Long) {
                    let long = new $util.Long(0, 0, true);
                    object.dataOffset = options.longs === String ? long.toString() : options.longs === Number ? long.toNumber() : long;
                } else
                    object.dataOffset = options.longs === String ? "0" : 0;
                if ($util.Long) {
                    let long = new $util.Long(0, 0, true);
                    object.dataLength = options.longs === String ? long.toString() : options.longs === Number ? long.toNumber() : long;
                } else
                    object.dataLength = options.longs === String ? "0" : 0;
                if ($util.Long) {
                    let long = new $util.Long(0, 0, true);
                    object.srcLength = options.longs === String ? long.toString() : options.longs === Number ? long.toNumber() : long;
                } else
                    object.srcLength = options.longs === String ? "0" : 0;
                if ($util.Long) {
                    let long = new $util.Long(0, 0, true);
                    object.dstLength = options.longs === String ? long.toString() : options.longs === Number ? long.toNumber() : long;
                } else
                    object.dstLength = options.longs === String ? "0" : 0;
                if (options.bytes === String)
                    object.dataSha256Hash = "";
                else {
                    object.dataSha256Hash = [];
                    if (options.bytes !== Array)
                        object.dataSha256Hash = $util.newBuffer(object.dataSha256Hash);
                }
                if (options.bytes === String)
                    object.srcSha256Hash = "";
                else {
                    object.srcSha256Hash = [];
                    if (options.bytes !== Array)
                        object.srcSha256Hash = $util.newBuffer(object.srcSha256Hash);
                }
            }
            if (message.type != null && message.hasOwnProperty("type"))
                object.type = options.enums === String ? $root.chromeos_update_engine.InstallOperation.Type[message.type] : message.type;
            if (message.dataOffset != null && message.hasOwnProperty("dataOffset"))
                if (typeof message.dataOffset === "number")
                    object.dataOffset = options.longs === String ? String(message.dataOffset) : message.dataOffset;
                else
                    object.dataOffset = options.longs === String ? $util.Long.prototype.toString.call(message.dataOffset) : options.longs === Number ? new $util.LongBits(message.dataOffset.low >>> 0, message.dataOffset.high >>> 0).toNumber(true) : message.dataOffset;
            if (message.dataLength != null && message.hasOwnProperty("dataLength"))
                if (typeof message.dataLength === "number")
                    object.dataLength = options.longs === String ? String(message.dataLength) : message.dataLength;
                else
                    object.dataLength = options.longs === String ? $util.Long.prototype.toString.call(message.dataLength) : options.longs === Number ? new $util.LongBits(message.dataLength.low >>> 0, message.dataLength.high >>> 0).toNumber(true) : message.dataLength;
            if (message.srcExtents && message.srcExtents.length) {
                object.srcExtents = [];
                for (let j = 0; j < message.srcExtents.length; ++j)
                    object.srcExtents[j] = $root.chromeos_update_engine.Extent.toObject(message.srcExtents[j], options);
            }
            if (message.srcLength != null && message.hasOwnProperty("srcLength"))
                if (typeof message.srcLength === "number")
                    object.srcLength = options.longs === String ? String(message.srcLength) : message.srcLength;
                else
                    object.srcLength = options.longs === String ? $util.Long.prototype.toString.call(message.srcLength) : options.longs === Number ? new $util.LongBits(message.srcLength.low >>> 0, message.srcLength.high >>> 0).toNumber(true) : message.srcLength;
            if (message.dstExtents && message.dstExtents.length) {
                object.dstExtents = [];
                for (let j = 0; j < message.dstExtents.length; ++j)
                    object.dstExtents[j] = $root.chromeos_update_engine.Extent.toObject(message.dstExtents[j], options);
            }
            if (message.dstLength != null && message.hasOwnProperty("dstLength"))
                if (typeof message.dstLength === "number")
                    object.dstLength = options.longs === String ? String(message.dstLength) : message.dstLength;
                else
                    object.dstLength = options.longs === String ? $util.Long.prototype.toString.call(message.dstLength) : options.longs === Number ? new $util.LongBits(message.dstLength.low >>> 0, message.dstLength.high >>> 0).toNumber(true) : message.dstLength;
            if (message.dataSha256Hash != null && message.hasOwnProperty("dataSha256Hash"))
                object.dataSha256Hash = options.bytes === String ? $util.base64.encode(message.dataSha256Hash, 0, message.dataSha256Hash.length) : options.bytes === Array ? Array.prototype.slice.call(message.dataSha256Hash) : message.dataSha256Hash;
            if (message.srcSha256Hash != null && message.hasOwnProperty("srcSha256Hash"))
                object.srcSha256Hash = options.bytes === String ? $util.base64.encode(message.srcSha256Hash, 0, message.srcSha256Hash.length) : options.bytes === Array ? Array.prototype.slice.call(message.srcSha256Hash) : message.srcSha256Hash;
            let keys2;
            if (message.xorMap && (keys2 = Object.keys(message.xorMap)).length) {
                object.xorMap = {};
                for (let j = 0; j < keys2.length; ++j)
                    object.xorMap[keys2[j]] = message.xorMap[keys2[j]];
            }
            return object;
        };

        /**
         * Converts this InstallOperation to JSON.
         * @function toJSON
         * @memberof chromeos_update_engine.InstallOperation
         * @instance
         * @returns {Object.<string,*>} JSON object
         */
        InstallOperation.prototype.toJSON = function toJSON() {
            return this.constructor.toObject(this, $protobuf.util.toJSONOptions);
        };

        /**
         * Type enum.
         * @name chromeos_update_engine.InstallOperation.Type
         * @enum {number}
         * @property {number} REPLACE=0 REPLACE value
         * @property {number} REPLACE_BZ=1 REPLACE_BZ value
         * @property {number} MOVE=2 MOVE value
         * @property {number} BSDIFF=3 BSDIFF value
         * @property {number} SOURCE_COPY=4 SOURCE_COPY value
         * @property {number} SOURCE_BSDIFF=5 SOURCE_BSDIFF value
         * @property {number} REPLACE_XZ=8 REPLACE_XZ value
         * @property {number} ZERO=6 ZERO value
         * @property {number} DISCARD=7 DISCARD value
         * @property {number} BROTLI_BSDIFF=10 BROTLI_BSDIFF value
         * @property {number} PUFFDIFF=9 PUFFDIFF value
         */
        InstallOperation.Type = (function() {
            const valuesById = {}, values = Object.create(valuesById);
            values[valuesById[0] = "REPLACE"] = 0;
            values[valuesById[1] = "REPLACE_BZ"] = 1;
            values[valuesById[2] = "MOVE"] = 2;
            values[valuesById[3] = "BSDIFF"] = 3;
            values[valuesById[4] = "SOURCE_COPY"] = 4;
            values[valuesById[5] = "SOURCE_BSDIFF"] = 5;
            values[valuesById[8] = "REPLACE_XZ"] = 8;
            values[valuesById[6] = "ZERO"] = 6;
            values[valuesById[7] = "DISCARD"] = 7;
            values[valuesById[10] = "BROTLI_BSDIFF"] = 10;
            values[valuesById[9] = "PUFFDIFF"] = 9;
            return values;
        })();

        return InstallOperation;
    })();

    chromeos_update_engine.CowMergeOperation = (function() {

        /**
         * Properties of a CowMergeOperation.
         * @memberof chromeos_update_engine
         * @interface ICowMergeOperation
         * @property {chromeos_update_engine.CowMergeOperation.Type|null} [type] CowMergeOperation type
         * @property {chromeos_update_engine.IExtent|null} [srcExtent] CowMergeOperation srcExtent
         * @property {chromeos_update_engine.IExtent|null} [dstExtent] CowMergeOperation dstExtent
         * @property {number|Long|null} [srcOffset] CowMergeOperation srcOffset
         */

        /**
         * Constructs a new CowMergeOperation.
         * @memberof chromeos_update_engine
         * @classdesc Represents a CowMergeOperation.
         * @implements ICowMergeOperation
         * @constructor
         * @param {chromeos_update_engine.ICowMergeOperation=} [properties] Properties to set
         */
        function CowMergeOperation(properties) {
            if (properties)
                for (let keys = Object.keys(properties), i = 0; i < keys.length; ++i)
                    if (properties[keys[i]] != null)
                        this[keys[i]] = properties[keys[i]];
        }

        /**
         * CowMergeOperation type.
         * @member {chromeos_update_engine.CowMergeOperation.Type} type
         * @memberof chromeos_update_engine.CowMergeOperation
         * @instance
         */
        CowMergeOperation.prototype.type = 0;

        /**
         * CowMergeOperation srcExtent.
         * @member {chromeos_update_engine.IExtent|null|undefined} srcExtent
         * @memberof chromeos_update_engine.CowMergeOperation
         * @instance
         */
        CowMergeOperation.prototype.srcExtent = null;

        /**
         * CowMergeOperation dstExtent.
         * @member {chromeos_update_engine.IExtent|null|undefined} dstExtent
         * @memberof chromeos_update_engine.CowMergeOperation
         * @instance
         */
        CowMergeOperation.prototype.dstExtent = null;

        /**
         * CowMergeOperation srcOffset.
         * @member {number|Long} srcOffset
         * @memberof chromeos_update_engine.CowMergeOperation
         * @instance
         */
        CowMergeOperation.prototype.srcOffset = $util.Long ? $util.Long.fromBits(0,0,true) : 0;

        /**
         * Creates a new CowMergeOperation instance using the specified properties.
         * @function create
         * @memberof chromeos_update_engine.CowMergeOperation
         * @static
         * @param {chromeos_update_engine.ICowMergeOperation=} [properties] Properties to set
         * @returns {chromeos_update_engine.CowMergeOperation} CowMergeOperation instance
         */
        CowMergeOperation.create = function create(properties) {
            return new CowMergeOperation(properties);
        };

        /**
         * Encodes the specified CowMergeOperation message. Does not implicitly {@link chromeos_update_engine.CowMergeOperation.verify|verify} messages.
         * @function encode
         * @memberof chromeos_update_engine.CowMergeOperation
         * @static
         * @param {chromeos_update_engine.ICowMergeOperation} message CowMergeOperation message or plain object to encode
         * @param {$protobuf.Writer} [writer] Writer to encode to
         * @returns {$protobuf.Writer} Writer
         */
        CowMergeOperation.encode = function encode(message, writer) {
            if (!writer)
                writer = $Writer.create();
            if (message.type != null && Object.hasOwnProperty.call(message, "type"))
                writer.uint32(/* id 1, wireType 0 =*/8).int32(message.type);
            if (message.srcExtent != null && Object.hasOwnProperty.call(message, "srcExtent"))
                $root.chromeos_update_engine.Extent.encode(message.srcExtent, writer.uint32(/* id 2, wireType 2 =*/18).fork()).ldelim();
            if (message.dstExtent != null && Object.hasOwnProperty.call(message, "dstExtent"))
                $root.chromeos_update_engine.Extent.encode(message.dstExtent, writer.uint32(/* id 3, wireType 2 =*/26).fork()).ldelim();
            if (message.srcOffset != null && Object.hasOwnProperty.call(message, "srcOffset"))
                writer.uint32(/* id 4, wireType 0 =*/32).uint64(message.srcOffset);
            return writer;
        };

        /**
         * Encodes the specified CowMergeOperation message, length delimited. Does not implicitly {@link chromeos_update_engine.CowMergeOperation.verify|verify} messages.
         * @function encodeDelimited
         * @memberof chromeos_update_engine.CowMergeOperation
         * @static
         * @param {chromeos_update_engine.ICowMergeOperation} message CowMergeOperation message or plain object to encode
         * @param {$protobuf.Writer} [writer] Writer to encode to
         * @returns {$protobuf.Writer} Writer
         */
        CowMergeOperation.encodeDelimited = function encodeDelimited(message, writer) {
            return this.encode(message, writer).ldelim();
        };

        /**
         * Decodes a CowMergeOperation message from the specified reader or buffer.
         * @function decode
         * @memberof chromeos_update_engine.CowMergeOperation
         * @static
         * @param {$protobuf.Reader|Uint8Array} reader Reader or buffer to decode from
         * @param {number} [length] Message length if known beforehand
         * @returns {chromeos_update_engine.CowMergeOperation} CowMergeOperation
         * @throws {Error} If the payload is not a reader or valid buffer
         * @throws {$protobuf.util.ProtocolError} If required fields are missing
         */
        CowMergeOperation.decode = function decode(reader, length) {
            if (!(reader instanceof $Reader))
                reader = $Reader.create(reader);
            let end = length === undefined ? reader.len : reader.pos + length, message = new $root.chromeos_update_engine.CowMergeOperation();
            while (reader.pos < end) {
                let tag = reader.uint32();
                switch (tag >>> 3) {
                case 1:
                    message.type = reader.int32();
                    break;
                case 2:
                    message.srcExtent = $root.chromeos_update_engine.Extent.decode(reader, reader.uint32());
                    break;
                case 3:
                    message.dstExtent = $root.chromeos_update_engine.Extent.decode(reader, reader.uint32());
                    break;
                case 4:
                    message.srcOffset = reader.uint64();
                    break;
                default:
                    reader.skipType(tag & 7);
                    break;
                }
            }
            return message;
        };

        /**
         * Decodes a CowMergeOperation message from the specified reader or buffer, length delimited.
         * @function decodeDelimited
         * @memberof chromeos_update_engine.CowMergeOperation
         * @static
         * @param {$protobuf.Reader|Uint8Array} reader Reader or buffer to decode from
         * @returns {chromeos_update_engine.CowMergeOperation} CowMergeOperation
         * @throws {Error} If the payload is not a reader or valid buffer
         * @throws {$protobuf.util.ProtocolError} If required fields are missing
         */
        CowMergeOperation.decodeDelimited = function decodeDelimited(reader) {
            if (!(reader instanceof $Reader))
                reader = new $Reader(reader);
            return this.decode(reader, reader.uint32());
        };

        /**
         * Verifies a CowMergeOperation message.
         * @function verify
         * @memberof chromeos_update_engine.CowMergeOperation
         * @static
         * @param {Object.<string,*>} message Plain object to verify
         * @returns {string|null} `null` if valid, otherwise the reason why it is not
         */
        CowMergeOperation.verify = function verify(message) {
            if (typeof message !== "object" || message === null)
                return "object expected";
            if (message.type != null && message.hasOwnProperty("type"))
                switch (message.type) {
                default:
                    return "type: enum value expected";
                case 0:
                case 1:
                case 2:
                    break;
                }
            if (message.srcExtent != null && message.hasOwnProperty("srcExtent")) {
                let error = $root.chromeos_update_engine.Extent.verify(message.srcExtent);
                if (error)
                    return "srcExtent." + error;
            }
            if (message.dstExtent != null && message.hasOwnProperty("dstExtent")) {
                let error = $root.chromeos_update_engine.Extent.verify(message.dstExtent);
                if (error)
                    return "dstExtent." + error;
            }
            if (message.srcOffset != null && message.hasOwnProperty("srcOffset"))
                if (!$util.isInteger(message.srcOffset) && !(message.srcOffset && $util.isInteger(message.srcOffset.low) && $util.isInteger(message.srcOffset.high)))
                    return "srcOffset: integer|Long expected";
            return null;
        };

        /**
         * Creates a CowMergeOperation message from a plain object. Also converts values to their respective internal types.
         * @function fromObject
         * @memberof chromeos_update_engine.CowMergeOperation
         * @static
         * @param {Object.<string,*>} object Plain object
         * @returns {chromeos_update_engine.CowMergeOperation} CowMergeOperation
         */
        CowMergeOperation.fromObject = function fromObject(object) {
            if (object instanceof $root.chromeos_update_engine.CowMergeOperation)
                return object;
            let message = new $root.chromeos_update_engine.CowMergeOperation();
            switch (object.type) {
            case "COW_COPY":
            case 0:
                message.type = 0;
                break;
            case "COW_XOR":
            case 1:
                message.type = 1;
                break;
            case "COW_REPLACE":
            case 2:
                message.type = 2;
                break;
            }
            if (object.srcExtent != null) {
                if (typeof object.srcExtent !== "object")
                    throw TypeError(".chromeos_update_engine.CowMergeOperation.srcExtent: object expected");
                message.srcExtent = $root.chromeos_update_engine.Extent.fromObject(object.srcExtent);
            }
            if (object.dstExtent != null) {
                if (typeof object.dstExtent !== "object")
                    throw TypeError(".chromeos_update_engine.CowMergeOperation.dstExtent: object expected");
                message.dstExtent = $root.chromeos_update_engine.Extent.fromObject(object.dstExtent);
            }
            if (object.srcOffset != null)
                if ($util.Long)
                    (message.srcOffset = $util.Long.fromValue(object.srcOffset)).unsigned = true;
                else if (typeof object.srcOffset === "string")
                    message.srcOffset = parseInt(object.srcOffset, 10);
                else if (typeof object.srcOffset === "number")
                    message.srcOffset = object.srcOffset;
                else if (typeof object.srcOffset === "object")
                    message.srcOffset = new $util.LongBits(object.srcOffset.low >>> 0, object.srcOffset.high >>> 0).toNumber(true);
            return message;
        };

        /**
         * Creates a plain object from a CowMergeOperation message. Also converts values to other types if specified.
         * @function toObject
         * @memberof chromeos_update_engine.CowMergeOperation
         * @static
         * @param {chromeos_update_engine.CowMergeOperation} message CowMergeOperation
         * @param {$protobuf.IConversionOptions} [options] Conversion options
         * @returns {Object.<string,*>} Plain object
         */
        CowMergeOperation.toObject = function toObject(message, options) {
            if (!options)
                options = {};
            let object = {};
            if (options.defaults) {
                object.type = options.enums === String ? "COW_COPY" : 0;
                object.srcExtent = null;
                object.dstExtent = null;
                if ($util.Long) {
                    let long = new $util.Long(0, 0, true);
                    object.srcOffset = options.longs === String ? long.toString() : options.longs === Number ? long.toNumber() : long;
                } else
                    object.srcOffset = options.longs === String ? "0" : 0;
            }
            if (message.type != null && message.hasOwnProperty("type"))
                object.type = options.enums === String ? $root.chromeos_update_engine.CowMergeOperation.Type[message.type] : message.type;
            if (message.srcExtent != null && message.hasOwnProperty("srcExtent"))
                object.srcExtent = $root.chromeos_update_engine.Extent.toObject(message.srcExtent, options);
            if (message.dstExtent != null && message.hasOwnProperty("dstExtent"))
                object.dstExtent = $root.chromeos_update_engine.Extent.toObject(message.dstExtent, options);
            if (message.srcOffset != null && message.hasOwnProperty("srcOffset"))
                if (typeof message.srcOffset === "number")
                    object.srcOffset = options.longs === String ? String(message.srcOffset) : message.srcOffset;
                else
                    object.srcOffset = options.longs === String ? $util.Long.prototype.toString.call(message.srcOffset) : options.longs === Number ? new $util.LongBits(message.srcOffset.low >>> 0, message.srcOffset.high >>> 0).toNumber(true) : message.srcOffset;
            return object;
        };

        /**
         * Converts this CowMergeOperation to JSON.
         * @function toJSON
         * @memberof chromeos_update_engine.CowMergeOperation
         * @instance
         * @returns {Object.<string,*>} JSON object
         */
        CowMergeOperation.prototype.toJSON = function toJSON() {
            return this.constructor.toObject(this, $protobuf.util.toJSONOptions);
        };

        /**
         * Type enum.
         * @name chromeos_update_engine.CowMergeOperation.Type
         * @enum {number}
         * @property {number} COW_COPY=0 COW_COPY value
         * @property {number} COW_XOR=1 COW_XOR value
         * @property {number} COW_REPLACE=2 COW_REPLACE value
         */
        CowMergeOperation.Type = (function() {
            const valuesById = {}, values = Object.create(valuesById);
            values[valuesById[0] = "COW_COPY"] = 0;
            values[valuesById[1] = "COW_XOR"] = 1;
            values[valuesById[2] = "COW_REPLACE"] = 2;
            return values;
        })();

        return CowMergeOperation;
    })();

    chromeos_update_engine.PartitionUpdate = (function() {

        /**
         * Properties of a PartitionUpdate.
         * @memberof chromeos_update_engine
         * @interface IPartitionUpdate
         * @property {string} partitionName PartitionUpdate partitionName
         * @property {boolean|null} [runPostinstall] PartitionUpdate runPostinstall
         * @property {string|null} [postinstallPath] PartitionUpdate postinstallPath
         * @property {string|null} [filesystemType] PartitionUpdate filesystemType
         * @property {Array.<chromeos_update_engine.Signatures.ISignature>|null} [newPartitionSignature] PartitionUpdate newPartitionSignature
         * @property {chromeos_update_engine.IPartitionInfo|null} [oldPartitionInfo] PartitionUpdate oldPartitionInfo
         * @property {chromeos_update_engine.IPartitionInfo|null} [newPartitionInfo] PartitionUpdate newPartitionInfo
         * @property {Array.<chromeos_update_engine.IInstallOperation>|null} [operations] PartitionUpdate operations
         * @property {boolean|null} [postinstallOptional] PartitionUpdate postinstallOptional
         * @property {chromeos_update_engine.IExtent|null} [hashTreeDataExtent] PartitionUpdate hashTreeDataExtent
         * @property {chromeos_update_engine.IExtent|null} [hashTreeExtent] PartitionUpdate hashTreeExtent
         * @property {string|null} [hashTreeAlgorithm] PartitionUpdate hashTreeAlgorithm
         * @property {Uint8Array|null} [hashTreeSalt] PartitionUpdate hashTreeSalt
         * @property {chromeos_update_engine.IExtent|null} [fecDataExtent] PartitionUpdate fecDataExtent
         * @property {chromeos_update_engine.IExtent|null} [fecExtent] PartitionUpdate fecExtent
         * @property {number|null} [fecRoots] PartitionUpdate fecRoots
         * @property {string|null} [version] PartitionUpdate version
         * @property {Array.<chromeos_update_engine.ICowMergeOperation>|null} [mergeOperations] PartitionUpdate mergeOperations
         * @property {number|Long|null} [estimateCowSize] PartitionUpdate estimateCowSize
         */

        /**
         * Constructs a new PartitionUpdate.
         * @memberof chromeos_update_engine
         * @classdesc Represents a PartitionUpdate.
         * @implements IPartitionUpdate
         * @constructor
         * @param {chromeos_update_engine.IPartitionUpdate=} [properties] Properties to set
         */
        function PartitionUpdate(properties) {
            this.newPartitionSignature = [];
            this.operations = [];
            this.mergeOperations = [];
            if (properties)
                for (let keys = Object.keys(properties), i = 0; i < keys.length; ++i)
                    if (properties[keys[i]] != null)
                        this[keys[i]] = properties[keys[i]];
        }

        /**
         * PartitionUpdate partitionName.
         * @member {string} partitionName
         * @memberof chromeos_update_engine.PartitionUpdate
         * @instance
         */
        PartitionUpdate.prototype.partitionName = "";

        /**
         * PartitionUpdate runPostinstall.
         * @member {boolean} runPostinstall
         * @memberof chromeos_update_engine.PartitionUpdate
         * @instance
         */
        PartitionUpdate.prototype.runPostinstall = false;

        /**
         * PartitionUpdate postinstallPath.
         * @member {string} postinstallPath
         * @memberof chromeos_update_engine.PartitionUpdate
         * @instance
         */
        PartitionUpdate.prototype.postinstallPath = "";

        /**
         * PartitionUpdate filesystemType.
         * @member {string} filesystemType
         * @memberof chromeos_update_engine.PartitionUpdate
         * @instance
         */
        PartitionUpdate.prototype.filesystemType = "";

        /**
         * PartitionUpdate newPartitionSignature.
         * @member {Array.<chromeos_update_engine.Signatures.ISignature>} newPartitionSignature
         * @memberof chromeos_update_engine.PartitionUpdate
         * @instance
         */
        PartitionUpdate.prototype.newPartitionSignature = $util.emptyArray;

        /**
         * PartitionUpdate oldPartitionInfo.
         * @member {chromeos_update_engine.IPartitionInfo|null|undefined} oldPartitionInfo
         * @memberof chromeos_update_engine.PartitionUpdate
         * @instance
         */
        PartitionUpdate.prototype.oldPartitionInfo = null;

        /**
         * PartitionUpdate newPartitionInfo.
         * @member {chromeos_update_engine.IPartitionInfo|null|undefined} newPartitionInfo
         * @memberof chromeos_update_engine.PartitionUpdate
         * @instance
         */
        PartitionUpdate.prototype.newPartitionInfo = null;

        /**
         * PartitionUpdate operations.
         * @member {Array.<chromeos_update_engine.IInstallOperation>} operations
         * @memberof chromeos_update_engine.PartitionUpdate
         * @instance
         */
        PartitionUpdate.prototype.operations = $util.emptyArray;

        /**
         * PartitionUpdate postinstallOptional.
         * @member {boolean} postinstallOptional
         * @memberof chromeos_update_engine.PartitionUpdate
         * @instance
         */
        PartitionUpdate.prototype.postinstallOptional = false;

        /**
         * PartitionUpdate hashTreeDataExtent.
         * @member {chromeos_update_engine.IExtent|null|undefined} hashTreeDataExtent
         * @memberof chromeos_update_engine.PartitionUpdate
         * @instance
         */
        PartitionUpdate.prototype.hashTreeDataExtent = null;

        /**
         * PartitionUpdate hashTreeExtent.
         * @member {chromeos_update_engine.IExtent|null|undefined} hashTreeExtent
         * @memberof chromeos_update_engine.PartitionUpdate
         * @instance
         */
        PartitionUpdate.prototype.hashTreeExtent = null;

        /**
         * PartitionUpdate hashTreeAlgorithm.
         * @member {string} hashTreeAlgorithm
         * @memberof chromeos_update_engine.PartitionUpdate
         * @instance
         */
        PartitionUpdate.prototype.hashTreeAlgorithm = "";

        /**
         * PartitionUpdate hashTreeSalt.
         * @member {Uint8Array} hashTreeSalt
         * @memberof chromeos_update_engine.PartitionUpdate
         * @instance
         */
        PartitionUpdate.prototype.hashTreeSalt = $util.newBuffer([]);

        /**
         * PartitionUpdate fecDataExtent.
         * @member {chromeos_update_engine.IExtent|null|undefined} fecDataExtent
         * @memberof chromeos_update_engine.PartitionUpdate
         * @instance
         */
        PartitionUpdate.prototype.fecDataExtent = null;

        /**
         * PartitionUpdate fecExtent.
         * @member {chromeos_update_engine.IExtent|null|undefined} fecExtent
         * @memberof chromeos_update_engine.PartitionUpdate
         * @instance
         */
        PartitionUpdate.prototype.fecExtent = null;

        /**
         * PartitionUpdate fecRoots.
         * @member {number} fecRoots
         * @memberof chromeos_update_engine.PartitionUpdate
         * @instance
         */
        PartitionUpdate.prototype.fecRoots = 2;

        /**
         * PartitionUpdate version.
         * @member {string} version
         * @memberof chromeos_update_engine.PartitionUpdate
         * @instance
         */
        PartitionUpdate.prototype.version = "";

        /**
         * PartitionUpdate mergeOperations.
         * @member {Array.<chromeos_update_engine.ICowMergeOperation>} mergeOperations
         * @memberof chromeos_update_engine.PartitionUpdate
         * @instance
         */
        PartitionUpdate.prototype.mergeOperations = $util.emptyArray;

        /**
         * PartitionUpdate estimateCowSize.
         * @member {number|Long} estimateCowSize
         * @memberof chromeos_update_engine.PartitionUpdate
         * @instance
         */
        PartitionUpdate.prototype.estimateCowSize = $util.Long ? $util.Long.fromBits(0,0,true) : 0;

        /**
         * Creates a new PartitionUpdate instance using the specified properties.
         * @function create
         * @memberof chromeos_update_engine.PartitionUpdate
         * @static
         * @param {chromeos_update_engine.IPartitionUpdate=} [properties] Properties to set
         * @returns {chromeos_update_engine.PartitionUpdate} PartitionUpdate instance
         */
        PartitionUpdate.create = function create(properties) {
            return new PartitionUpdate(properties);
        };

        /**
         * Encodes the specified PartitionUpdate message. Does not implicitly {@link chromeos_update_engine.PartitionUpdate.verify|verify} messages.
         * @function encode
         * @memberof chromeos_update_engine.PartitionUpdate
         * @static
         * @param {chromeos_update_engine.IPartitionUpdate} message PartitionUpdate message or plain object to encode
         * @param {$protobuf.Writer} [writer] Writer to encode to
         * @returns {$protobuf.Writer} Writer
         */
        PartitionUpdate.encode = function encode(message, writer) {
            if (!writer)
                writer = $Writer.create();
            writer.uint32(/* id 1, wireType 2 =*/10).string(message.partitionName);
            if (message.runPostinstall != null && Object.hasOwnProperty.call(message, "runPostinstall"))
                writer.uint32(/* id 2, wireType 0 =*/16).bool(message.runPostinstall);
            if (message.postinstallPath != null && Object.hasOwnProperty.call(message, "postinstallPath"))
                writer.uint32(/* id 3, wireType 2 =*/26).string(message.postinstallPath);
            if (message.filesystemType != null && Object.hasOwnProperty.call(message, "filesystemType"))
                writer.uint32(/* id 4, wireType 2 =*/34).string(message.filesystemType);
            if (message.newPartitionSignature != null && message.newPartitionSignature.length)
                for (let i = 0; i < message.newPartitionSignature.length; ++i)
                    $root.chromeos_update_engine.Signatures.Signature.encode(message.newPartitionSignature[i], writer.uint32(/* id 5, wireType 2 =*/42).fork()).ldelim();
            if (message.oldPartitionInfo != null && Object.hasOwnProperty.call(message, "oldPartitionInfo"))
                $root.chromeos_update_engine.PartitionInfo.encode(message.oldPartitionInfo, writer.uint32(/* id 6, wireType 2 =*/50).fork()).ldelim();
            if (message.newPartitionInfo != null && Object.hasOwnProperty.call(message, "newPartitionInfo"))
                $root.chromeos_update_engine.PartitionInfo.encode(message.newPartitionInfo, writer.uint32(/* id 7, wireType 2 =*/58).fork()).ldelim();
            if (message.operations != null && message.operations.length)
                for (let i = 0; i < message.operations.length; ++i)
                    $root.chromeos_update_engine.InstallOperation.encode(message.operations[i], writer.uint32(/* id 8, wireType 2 =*/66).fork()).ldelim();
            if (message.postinstallOptional != null && Object.hasOwnProperty.call(message, "postinstallOptional"))
                writer.uint32(/* id 9, wireType 0 =*/72).bool(message.postinstallOptional);
            if (message.hashTreeDataExtent != null && Object.hasOwnProperty.call(message, "hashTreeDataExtent"))
                $root.chromeos_update_engine.Extent.encode(message.hashTreeDataExtent, writer.uint32(/* id 10, wireType 2 =*/82).fork()).ldelim();
            if (message.hashTreeExtent != null && Object.hasOwnProperty.call(message, "hashTreeExtent"))
                $root.chromeos_update_engine.Extent.encode(message.hashTreeExtent, writer.uint32(/* id 11, wireType 2 =*/90).fork()).ldelim();
            if (message.hashTreeAlgorithm != null && Object.hasOwnProperty.call(message, "hashTreeAlgorithm"))
                writer.uint32(/* id 12, wireType 2 =*/98).string(message.hashTreeAlgorithm);
            if (message.hashTreeSalt != null && Object.hasOwnProperty.call(message, "hashTreeSalt"))
                writer.uint32(/* id 13, wireType 2 =*/106).bytes(message.hashTreeSalt);
            if (message.fecDataExtent != null && Object.hasOwnProperty.call(message, "fecDataExtent"))
                $root.chromeos_update_engine.Extent.encode(message.fecDataExtent, writer.uint32(/* id 14, wireType 2 =*/114).fork()).ldelim();
            if (message.fecExtent != null && Object.hasOwnProperty.call(message, "fecExtent"))
                $root.chromeos_update_engine.Extent.encode(message.fecExtent, writer.uint32(/* id 15, wireType 2 =*/122).fork()).ldelim();
            if (message.fecRoots != null && Object.hasOwnProperty.call(message, "fecRoots"))
                writer.uint32(/* id 16, wireType 0 =*/128).uint32(message.fecRoots);
            if (message.version != null && Object.hasOwnProperty.call(message, "version"))
                writer.uint32(/* id 17, wireType 2 =*/138).string(message.version);
            if (message.mergeOperations != null && message.mergeOperations.length)
                for (let i = 0; i < message.mergeOperations.length; ++i)
                    $root.chromeos_update_engine.CowMergeOperation.encode(message.mergeOperations[i], writer.uint32(/* id 18, wireType 2 =*/146).fork()).ldelim();
            if (message.estimateCowSize != null && Object.hasOwnProperty.call(message, "estimateCowSize"))
                writer.uint32(/* id 19, wireType 0 =*/152).uint64(message.estimateCowSize);
            return writer;
        };

        /**
         * Encodes the specified PartitionUpdate message, length delimited. Does not implicitly {@link chromeos_update_engine.PartitionUpdate.verify|verify} messages.
         * @function encodeDelimited
         * @memberof chromeos_update_engine.PartitionUpdate
         * @static
         * @param {chromeos_update_engine.IPartitionUpdate} message PartitionUpdate message or plain object to encode
         * @param {$protobuf.Writer} [writer] Writer to encode to
         * @returns {$protobuf.Writer} Writer
         */
        PartitionUpdate.encodeDelimited = function encodeDelimited(message, writer) {
            return this.encode(message, writer).ldelim();
        };

        /**
         * Decodes a PartitionUpdate message from the specified reader or buffer.
         * @function decode
         * @memberof chromeos_update_engine.PartitionUpdate
         * @static
         * @param {$protobuf.Reader|Uint8Array} reader Reader or buffer to decode from
         * @param {number} [length] Message length if known beforehand
         * @returns {chromeos_update_engine.PartitionUpdate} PartitionUpdate
         * @throws {Error} If the payload is not a reader or valid buffer
         * @throws {$protobuf.util.ProtocolError} If required fields are missing
         */
        PartitionUpdate.decode = function decode(reader, length) {
            if (!(reader instanceof $Reader))
                reader = $Reader.create(reader);
            let end = length === undefined ? reader.len : reader.pos + length, message = new $root.chromeos_update_engine.PartitionUpdate();
            while (reader.pos < end) {
                let tag = reader.uint32();
                switch (tag >>> 3) {
                case 1:
                    message.partitionName = reader.string();
                    break;
                case 2:
                    message.runPostinstall = reader.bool();
                    break;
                case 3:
                    message.postinstallPath = reader.string();
                    break;
                case 4:
                    message.filesystemType = reader.string();
                    break;
                case 5:
                    if (!(message.newPartitionSignature && message.newPartitionSignature.length))
                        message.newPartitionSignature = [];
                    message.newPartitionSignature.push($root.chromeos_update_engine.Signatures.Signature.decode(reader, reader.uint32()));
                    break;
                case 6:
                    message.oldPartitionInfo = $root.chromeos_update_engine.PartitionInfo.decode(reader, reader.uint32());
                    break;
                case 7:
                    message.newPartitionInfo = $root.chromeos_update_engine.PartitionInfo.decode(reader, reader.uint32());
                    break;
                case 8:
                    if (!(message.operations && message.operations.length))
                        message.operations = [];
                    message.operations.push($root.chromeos_update_engine.InstallOperation.decode(reader, reader.uint32()));
                    break;
                case 9:
                    message.postinstallOptional = reader.bool();
                    break;
                case 10:
                    message.hashTreeDataExtent = $root.chromeos_update_engine.Extent.decode(reader, reader.uint32());
                    break;
                case 11:
                    message.hashTreeExtent = $root.chromeos_update_engine.Extent.decode(reader, reader.uint32());
                    break;
                case 12:
                    message.hashTreeAlgorithm = reader.string();
                    break;
                case 13:
                    message.hashTreeSalt = reader.bytes();
                    break;
                case 14:
                    message.fecDataExtent = $root.chromeos_update_engine.Extent.decode(reader, reader.uint32());
                    break;
                case 15:
                    message.fecExtent = $root.chromeos_update_engine.Extent.decode(reader, reader.uint32());
                    break;
                case 16:
                    message.fecRoots = reader.uint32();
                    break;
                case 17:
                    message.version = reader.string();
                    break;
                case 18:
                    if (!(message.mergeOperations && message.mergeOperations.length))
                        message.mergeOperations = [];
                    message.mergeOperations.push($root.chromeos_update_engine.CowMergeOperation.decode(reader, reader.uint32()));
                    break;
                case 19:
                    message.estimateCowSize = reader.uint64();
                    break;
                default:
                    reader.skipType(tag & 7);
                    break;
                }
            }
            if (!message.hasOwnProperty("partitionName"))
                throw $util.ProtocolError("missing required 'partitionName'", { instance: message });
            return message;
        };

        /**
         * Decodes a PartitionUpdate message from the specified reader or buffer, length delimited.
         * @function decodeDelimited
         * @memberof chromeos_update_engine.PartitionUpdate
         * @static
         * @param {$protobuf.Reader|Uint8Array} reader Reader or buffer to decode from
         * @returns {chromeos_update_engine.PartitionUpdate} PartitionUpdate
         * @throws {Error} If the payload is not a reader or valid buffer
         * @throws {$protobuf.util.ProtocolError} If required fields are missing
         */
        PartitionUpdate.decodeDelimited = function decodeDelimited(reader) {
            if (!(reader instanceof $Reader))
                reader = new $Reader(reader);
            return this.decode(reader, reader.uint32());
        };

        /**
         * Verifies a PartitionUpdate message.
         * @function verify
         * @memberof chromeos_update_engine.PartitionUpdate
         * @static
         * @param {Object.<string,*>} message Plain object to verify
         * @returns {string|null} `null` if valid, otherwise the reason why it is not
         */
        PartitionUpdate.verify = function verify(message) {
            if (typeof message !== "object" || message === null)
                return "object expected";
            if (!$util.isString(message.partitionName))
                return "partitionName: string expected";
            if (message.runPostinstall != null && message.hasOwnProperty("runPostinstall"))
                if (typeof message.runPostinstall !== "boolean")
                    return "runPostinstall: boolean expected";
            if (message.postinstallPath != null && message.hasOwnProperty("postinstallPath"))
                if (!$util.isString(message.postinstallPath))
                    return "postinstallPath: string expected";
            if (message.filesystemType != null && message.hasOwnProperty("filesystemType"))
                if (!$util.isString(message.filesystemType))
                    return "filesystemType: string expected";
            if (message.newPartitionSignature != null && message.hasOwnProperty("newPartitionSignature")) {
                if (!Array.isArray(message.newPartitionSignature))
                    return "newPartitionSignature: array expected";
                for (let i = 0; i < message.newPartitionSignature.length; ++i) {
                    let error = $root.chromeos_update_engine.Signatures.Signature.verify(message.newPartitionSignature[i]);
                    if (error)
                        return "newPartitionSignature." + error;
                }
            }
            if (message.oldPartitionInfo != null && message.hasOwnProperty("oldPartitionInfo")) {
                let error = $root.chromeos_update_engine.PartitionInfo.verify(message.oldPartitionInfo);
                if (error)
                    return "oldPartitionInfo." + error;
            }
            if (message.newPartitionInfo != null && message.hasOwnProperty("newPartitionInfo")) {
                let error = $root.chromeos_update_engine.PartitionInfo.verify(message.newPartitionInfo);
                if (error)
                    return "newPartitionInfo." + error;
            }
            if (message.operations != null && message.hasOwnProperty("operations")) {
                if (!Array.isArray(message.operations))
                    return "operations: array expected";
                for (let i = 0; i < message.operations.length; ++i) {
                    let error = $root.chromeos_update_engine.InstallOperation.verify(message.operations[i]);
                    if (error)
                        return "operations." + error;
                }
            }
            if (message.postinstallOptional != null && message.hasOwnProperty("postinstallOptional"))
                if (typeof message.postinstallOptional !== "boolean")
                    return "postinstallOptional: boolean expected";
            if (message.hashTreeDataExtent != null && message.hasOwnProperty("hashTreeDataExtent")) {
                let error = $root.chromeos_update_engine.Extent.verify(message.hashTreeDataExtent);
                if (error)
                    return "hashTreeDataExtent." + error;
            }
            if (message.hashTreeExtent != null && message.hasOwnProperty("hashTreeExtent")) {
                let error = $root.chromeos_update_engine.Extent.verify(message.hashTreeExtent);
                if (error)
                    return "hashTreeExtent." + error;
            }
            if (message.hashTreeAlgorithm != null && message.hasOwnProperty("hashTreeAlgorithm"))
                if (!$util.isString(message.hashTreeAlgorithm))
                    return "hashTreeAlgorithm: string expected";
            if (message.hashTreeSalt != null && message.hasOwnProperty("hashTreeSalt"))
                if (!(message.hashTreeSalt && typeof message.hashTreeSalt.length === "number" || $util.isString(message.hashTreeSalt)))
                    return "hashTreeSalt: buffer expected";
            if (message.fecDataExtent != null && message.hasOwnProperty("fecDataExtent")) {
                let error = $root.chromeos_update_engine.Extent.verify(message.fecDataExtent);
                if (error)
                    return "fecDataExtent." + error;
            }
            if (message.fecExtent != null && message.hasOwnProperty("fecExtent")) {
                let error = $root.chromeos_update_engine.Extent.verify(message.fecExtent);
                if (error)
                    return "fecExtent." + error;
            }
            if (message.fecRoots != null && message.hasOwnProperty("fecRoots"))
                if (!$util.isInteger(message.fecRoots))
                    return "fecRoots: integer expected";
            if (message.version != null && message.hasOwnProperty("version"))
                if (!$util.isString(message.version))
                    return "version: string expected";
            if (message.mergeOperations != null && message.hasOwnProperty("mergeOperations")) {
                if (!Array.isArray(message.mergeOperations))
                    return "mergeOperations: array expected";
                for (let i = 0; i < message.mergeOperations.length; ++i) {
                    let error = $root.chromeos_update_engine.CowMergeOperation.verify(message.mergeOperations[i]);
                    if (error)
                        return "mergeOperations." + error;
                }
            }
            if (message.estimateCowSize != null && message.hasOwnProperty("estimateCowSize"))
                if (!$util.isInteger(message.estimateCowSize) && !(message.estimateCowSize && $util.isInteger(message.estimateCowSize.low) && $util.isInteger(message.estimateCowSize.high)))
                    return "estimateCowSize: integer|Long expected";
            return null;
        };

        /**
         * Creates a PartitionUpdate message from a plain object. Also converts values to their respective internal types.
         * @function fromObject
         * @memberof chromeos_update_engine.PartitionUpdate
         * @static
         * @param {Object.<string,*>} object Plain object
         * @returns {chromeos_update_engine.PartitionUpdate} PartitionUpdate
         */
        PartitionUpdate.fromObject = function fromObject(object) {
            if (object instanceof $root.chromeos_update_engine.PartitionUpdate)
                return object;
            let message = new $root.chromeos_update_engine.PartitionUpdate();
            if (object.partitionName != null)
                message.partitionName = String(object.partitionName);
            if (object.runPostinstall != null)
                message.runPostinstall = Boolean(object.runPostinstall);
            if (object.postinstallPath != null)
                message.postinstallPath = String(object.postinstallPath);
            if (object.filesystemType != null)
                message.filesystemType = String(object.filesystemType);
            if (object.newPartitionSignature) {
                if (!Array.isArray(object.newPartitionSignature))
                    throw TypeError(".chromeos_update_engine.PartitionUpdate.newPartitionSignature: array expected");
                message.newPartitionSignature = [];
                for (let i = 0; i < object.newPartitionSignature.length; ++i) {
                    if (typeof object.newPartitionSignature[i] !== "object")
                        throw TypeError(".chromeos_update_engine.PartitionUpdate.newPartitionSignature: object expected");
                    message.newPartitionSignature[i] = $root.chromeos_update_engine.Signatures.Signature.fromObject(object.newPartitionSignature[i]);
                }
            }
            if (object.oldPartitionInfo != null) {
                if (typeof object.oldPartitionInfo !== "object")
                    throw TypeError(".chromeos_update_engine.PartitionUpdate.oldPartitionInfo: object expected");
                message.oldPartitionInfo = $root.chromeos_update_engine.PartitionInfo.fromObject(object.oldPartitionInfo);
            }
            if (object.newPartitionInfo != null) {
                if (typeof object.newPartitionInfo !== "object")
                    throw TypeError(".chromeos_update_engine.PartitionUpdate.newPartitionInfo: object expected");
                message.newPartitionInfo = $root.chromeos_update_engine.PartitionInfo.fromObject(object.newPartitionInfo);
            }
            if (object.operations) {
                if (!Array.isArray(object.operations))
                    throw TypeError(".chromeos_update_engine.PartitionUpdate.operations: array expected");
                message.operations = [];
                for (let i = 0; i < object.operations.length; ++i) {
                    if (typeof object.operations[i] !== "object")
                        throw TypeError(".chromeos_update_engine.PartitionUpdate.operations: object expected");
                    message.operations[i] = $root.chromeos_update_engine.InstallOperation.fromObject(object.operations[i]);
                }
            }
            if (object.postinstallOptional != null)
                message.postinstallOptional = Boolean(object.postinstallOptional);
            if (object.hashTreeDataExtent != null) {
                if (typeof object.hashTreeDataExtent !== "object")
                    throw TypeError(".chromeos_update_engine.PartitionUpdate.hashTreeDataExtent: object expected");
                message.hashTreeDataExtent = $root.chromeos_update_engine.Extent.fromObject(object.hashTreeDataExtent);
            }
            if (object.hashTreeExtent != null) {
                if (typeof object.hashTreeExtent !== "object")
                    throw TypeError(".chromeos_update_engine.PartitionUpdate.hashTreeExtent: object expected");
                message.hashTreeExtent = $root.chromeos_update_engine.Extent.fromObject(object.hashTreeExtent);
            }
            if (object.hashTreeAlgorithm != null)
                message.hashTreeAlgorithm = String(object.hashTreeAlgorithm);
            if (object.hashTreeSalt != null)
                if (typeof object.hashTreeSalt === "string")
                    $util.base64.decode(object.hashTreeSalt, message.hashTreeSalt = $util.newBuffer($util.base64.length(object.hashTreeSalt)), 0);
                else if (object.hashTreeSalt.length)
                    message.hashTreeSalt = object.hashTreeSalt;
            if (object.fecDataExtent != null) {
                if (typeof object.fecDataExtent !== "object")
                    throw TypeError(".chromeos_update_engine.PartitionUpdate.fecDataExtent: object expected");
                message.fecDataExtent = $root.chromeos_update_engine.Extent.fromObject(object.fecDataExtent);
            }
            if (object.fecExtent != null) {
                if (typeof object.fecExtent !== "object")
                    throw TypeError(".chromeos_update_engine.PartitionUpdate.fecExtent: object expected");
                message.fecExtent = $root.chromeos_update_engine.Extent.fromObject(object.fecExtent);
            }
            if (object.fecRoots != null)
                message.fecRoots = object.fecRoots >>> 0;
            if (object.version != null)
                message.version = String(object.version);
            if (object.mergeOperations) {
                if (!Array.isArray(object.mergeOperations))
                    throw TypeError(".chromeos_update_engine.PartitionUpdate.mergeOperations: array expected");
                message.mergeOperations = [];
                for (let i = 0; i < object.mergeOperations.length; ++i) {
                    if (typeof object.mergeOperations[i] !== "object")
                        throw TypeError(".chromeos_update_engine.PartitionUpdate.mergeOperations: object expected");
                    message.mergeOperations[i] = $root.chromeos_update_engine.CowMergeOperation.fromObject(object.mergeOperations[i]);
                }
            }
            if (object.estimateCowSize != null)
                if ($util.Long)
                    (message.estimateCowSize = $util.Long.fromValue(object.estimateCowSize)).unsigned = true;
                else if (typeof object.estimateCowSize === "string")
                    message.estimateCowSize = parseInt(object.estimateCowSize, 10);
                else if (typeof object.estimateCowSize === "number")
                    message.estimateCowSize = object.estimateCowSize;
                else if (typeof object.estimateCowSize === "object")
                    message.estimateCowSize = new $util.LongBits(object.estimateCowSize.low >>> 0, object.estimateCowSize.high >>> 0).toNumber(true);
            return message;
        };

        /**
         * Creates a plain object from a PartitionUpdate message. Also converts values to other types if specified.
         * @function toObject
         * @memberof chromeos_update_engine.PartitionUpdate
         * @static
         * @param {chromeos_update_engine.PartitionUpdate} message PartitionUpdate
         * @param {$protobuf.IConversionOptions} [options] Conversion options
         * @returns {Object.<string,*>} Plain object
         */
        PartitionUpdate.toObject = function toObject(message, options) {
            if (!options)
                options = {};
            let object = {};
            if (options.arrays || options.defaults) {
                object.newPartitionSignature = [];
                object.operations = [];
                object.mergeOperations = [];
            }
            if (options.defaults) {
                object.partitionName = "";
                object.runPostinstall = false;
                object.postinstallPath = "";
                object.filesystemType = "";
                object.oldPartitionInfo = null;
                object.newPartitionInfo = null;
                object.postinstallOptional = false;
                object.hashTreeDataExtent = null;
                object.hashTreeExtent = null;
                object.hashTreeAlgorithm = "";
                if (options.bytes === String)
                    object.hashTreeSalt = "";
                else {
                    object.hashTreeSalt = [];
                    if (options.bytes !== Array)
                        object.hashTreeSalt = $util.newBuffer(object.hashTreeSalt);
                }
                object.fecDataExtent = null;
                object.fecExtent = null;
                object.fecRoots = 2;
                object.version = "";
                if ($util.Long) {
                    let long = new $util.Long(0, 0, true);
                    object.estimateCowSize = options.longs === String ? long.toString() : options.longs === Number ? long.toNumber() : long;
                } else
                    object.estimateCowSize = options.longs === String ? "0" : 0;
            }
            if (message.partitionName != null && message.hasOwnProperty("partitionName"))
                object.partitionName = message.partitionName;
            if (message.runPostinstall != null && message.hasOwnProperty("runPostinstall"))
                object.runPostinstall = message.runPostinstall;
            if (message.postinstallPath != null && message.hasOwnProperty("postinstallPath"))
                object.postinstallPath = message.postinstallPath;
            if (message.filesystemType != null && message.hasOwnProperty("filesystemType"))
                object.filesystemType = message.filesystemType;
            if (message.newPartitionSignature && message.newPartitionSignature.length) {
                object.newPartitionSignature = [];
                for (let j = 0; j < message.newPartitionSignature.length; ++j)
                    object.newPartitionSignature[j] = $root.chromeos_update_engine.Signatures.Signature.toObject(message.newPartitionSignature[j], options);
            }
            if (message.oldPartitionInfo != null && message.hasOwnProperty("oldPartitionInfo"))
                object.oldPartitionInfo = $root.chromeos_update_engine.PartitionInfo.toObject(message.oldPartitionInfo, options);
            if (message.newPartitionInfo != null && message.hasOwnProperty("newPartitionInfo"))
                object.newPartitionInfo = $root.chromeos_update_engine.PartitionInfo.toObject(message.newPartitionInfo, options);
            if (message.operations && message.operations.length) {
                object.operations = [];
                for (let j = 0; j < message.operations.length; ++j)
                    object.operations[j] = $root.chromeos_update_engine.InstallOperation.toObject(message.operations[j], options);
            }
            if (message.postinstallOptional != null && message.hasOwnProperty("postinstallOptional"))
                object.postinstallOptional = message.postinstallOptional;
            if (message.hashTreeDataExtent != null && message.hasOwnProperty("hashTreeDataExtent"))
                object.hashTreeDataExtent = $root.chromeos_update_engine.Extent.toObject(message.hashTreeDataExtent, options);
            if (message.hashTreeExtent != null && message.hasOwnProperty("hashTreeExtent"))
                object.hashTreeExtent = $root.chromeos_update_engine.Extent.toObject(message.hashTreeExtent, options);
            if (message.hashTreeAlgorithm != null && message.hasOwnProperty("hashTreeAlgorithm"))
                object.hashTreeAlgorithm = message.hashTreeAlgorithm;
            if (message.hashTreeSalt != null && message.hasOwnProperty("hashTreeSalt"))
                object.hashTreeSalt = options.bytes === String ? $util.base64.encode(message.hashTreeSalt, 0, message.hashTreeSalt.length) : options.bytes === Array ? Array.prototype.slice.call(message.hashTreeSalt) : message.hashTreeSalt;
            if (message.fecDataExtent != null && message.hasOwnProperty("fecDataExtent"))
                object.fecDataExtent = $root.chromeos_update_engine.Extent.toObject(message.fecDataExtent, options);
            if (message.fecExtent != null && message.hasOwnProperty("fecExtent"))
                object.fecExtent = $root.chromeos_update_engine.Extent.toObject(message.fecExtent, options);
            if (message.fecRoots != null && message.hasOwnProperty("fecRoots"))
                object.fecRoots = message.fecRoots;
            if (message.version != null && message.hasOwnProperty("version"))
                object.version = message.version;
            if (message.mergeOperations && message.mergeOperations.length) {
                object.mergeOperations = [];
                for (let j = 0; j < message.mergeOperations.length; ++j)
                    object.mergeOperations[j] = $root.chromeos_update_engine.CowMergeOperation.toObject(message.mergeOperations[j], options);
            }
            if (message.estimateCowSize != null && message.hasOwnProperty("estimateCowSize"))
                if (typeof message.estimateCowSize === "number")
                    object.estimateCowSize = options.longs === String ? String(message.estimateCowSize) : message.estimateCowSize;
                else
                    object.estimateCowSize = options.longs === String ? $util.Long.prototype.toString.call(message.estimateCowSize) : options.longs === Number ? new $util.LongBits(message.estimateCowSize.low >>> 0, message.estimateCowSize.high >>> 0).toNumber(true) : message.estimateCowSize;
            return object;
        };

        /**
         * Converts this PartitionUpdate to JSON.
         * @function toJSON
         * @memberof chromeos_update_engine.PartitionUpdate
         * @instance
         * @returns {Object.<string,*>} JSON object
         */
        PartitionUpdate.prototype.toJSON = function toJSON() {
            return this.constructor.toObject(this, $protobuf.util.toJSONOptions);
        };

        return PartitionUpdate;
    })();

    chromeos_update_engine.DynamicPartitionGroup = (function() {

        /**
         * Properties of a DynamicPartitionGroup.
         * @memberof chromeos_update_engine
         * @interface IDynamicPartitionGroup
         * @property {string} name DynamicPartitionGroup name
         * @property {number|Long|null} [size] DynamicPartitionGroup size
         * @property {Array.<string>|null} [partitionNames] DynamicPartitionGroup partitionNames
         */

        /**
         * Constructs a new DynamicPartitionGroup.
         * @memberof chromeos_update_engine
         * @classdesc Represents a DynamicPartitionGroup.
         * @implements IDynamicPartitionGroup
         * @constructor
         * @param {chromeos_update_engine.IDynamicPartitionGroup=} [properties] Properties to set
         */
        function DynamicPartitionGroup(properties) {
            this.partitionNames = [];
            if (properties)
                for (let keys = Object.keys(properties), i = 0; i < keys.length; ++i)
                    if (properties[keys[i]] != null)
                        this[keys[i]] = properties[keys[i]];
        }

        /**
         * DynamicPartitionGroup name.
         * @member {string} name
         * @memberof chromeos_update_engine.DynamicPartitionGroup
         * @instance
         */
        DynamicPartitionGroup.prototype.name = "";

        /**
         * DynamicPartitionGroup size.
         * @member {number|Long} size
         * @memberof chromeos_update_engine.DynamicPartitionGroup
         * @instance
         */
        DynamicPartitionGroup.prototype.size = $util.Long ? $util.Long.fromBits(0,0,true) : 0;

        /**
         * DynamicPartitionGroup partitionNames.
         * @member {Array.<string>} partitionNames
         * @memberof chromeos_update_engine.DynamicPartitionGroup
         * @instance
         */
        DynamicPartitionGroup.prototype.partitionNames = $util.emptyArray;

        /**
         * Creates a new DynamicPartitionGroup instance using the specified properties.
         * @function create
         * @memberof chromeos_update_engine.DynamicPartitionGroup
         * @static
         * @param {chromeos_update_engine.IDynamicPartitionGroup=} [properties] Properties to set
         * @returns {chromeos_update_engine.DynamicPartitionGroup} DynamicPartitionGroup instance
         */
        DynamicPartitionGroup.create = function create(properties) {
            return new DynamicPartitionGroup(properties);
        };

        /**
         * Encodes the specified DynamicPartitionGroup message. Does not implicitly {@link chromeos_update_engine.DynamicPartitionGroup.verify|verify} messages.
         * @function encode
         * @memberof chromeos_update_engine.DynamicPartitionGroup
         * @static
         * @param {chromeos_update_engine.IDynamicPartitionGroup} message DynamicPartitionGroup message or plain object to encode
         * @param {$protobuf.Writer} [writer] Writer to encode to
         * @returns {$protobuf.Writer} Writer
         */
        DynamicPartitionGroup.encode = function encode(message, writer) {
            if (!writer)
                writer = $Writer.create();
            writer.uint32(/* id 1, wireType 2 =*/10).string(message.name);
            if (message.size != null && Object.hasOwnProperty.call(message, "size"))
                writer.uint32(/* id 2, wireType 0 =*/16).uint64(message.size);
            if (message.partitionNames != null && message.partitionNames.length)
                for (let i = 0; i < message.partitionNames.length; ++i)
                    writer.uint32(/* id 3, wireType 2 =*/26).string(message.partitionNames[i]);
            return writer;
        };

        /**
         * Encodes the specified DynamicPartitionGroup message, length delimited. Does not implicitly {@link chromeos_update_engine.DynamicPartitionGroup.verify|verify} messages.
         * @function encodeDelimited
         * @memberof chromeos_update_engine.DynamicPartitionGroup
         * @static
         * @param {chromeos_update_engine.IDynamicPartitionGroup} message DynamicPartitionGroup message or plain object to encode
         * @param {$protobuf.Writer} [writer] Writer to encode to
         * @returns {$protobuf.Writer} Writer
         */
        DynamicPartitionGroup.encodeDelimited = function encodeDelimited(message, writer) {
            return this.encode(message, writer).ldelim();
        };

        /**
         * Decodes a DynamicPartitionGroup message from the specified reader or buffer.
         * @function decode
         * @memberof chromeos_update_engine.DynamicPartitionGroup
         * @static
         * @param {$protobuf.Reader|Uint8Array} reader Reader or buffer to decode from
         * @param {number} [length] Message length if known beforehand
         * @returns {chromeos_update_engine.DynamicPartitionGroup} DynamicPartitionGroup
         * @throws {Error} If the payload is not a reader or valid buffer
         * @throws {$protobuf.util.ProtocolError} If required fields are missing
         */
        DynamicPartitionGroup.decode = function decode(reader, length) {
            if (!(reader instanceof $Reader))
                reader = $Reader.create(reader);
            let end = length === undefined ? reader.len : reader.pos + length, message = new $root.chromeos_update_engine.DynamicPartitionGroup();
            while (reader.pos < end) {
                let tag = reader.uint32();
                switch (tag >>> 3) {
                case 1:
                    message.name = reader.string();
                    break;
                case 2:
                    message.size = reader.uint64();
                    break;
                case 3:
                    if (!(message.partitionNames && message.partitionNames.length))
                        message.partitionNames = [];
                    message.partitionNames.push(reader.string());
                    break;
                default:
                    reader.skipType(tag & 7);
                    break;
                }
            }
            if (!message.hasOwnProperty("name"))
                throw $util.ProtocolError("missing required 'name'", { instance: message });
            return message;
        };

        /**
         * Decodes a DynamicPartitionGroup message from the specified reader or buffer, length delimited.
         * @function decodeDelimited
         * @memberof chromeos_update_engine.DynamicPartitionGroup
         * @static
         * @param {$protobuf.Reader|Uint8Array} reader Reader or buffer to decode from
         * @returns {chromeos_update_engine.DynamicPartitionGroup} DynamicPartitionGroup
         * @throws {Error} If the payload is not a reader or valid buffer
         * @throws {$protobuf.util.ProtocolError} If required fields are missing
         */
        DynamicPartitionGroup.decodeDelimited = function decodeDelimited(reader) {
            if (!(reader instanceof $Reader))
                reader = new $Reader(reader);
            return this.decode(reader, reader.uint32());
        };

        /**
         * Verifies a DynamicPartitionGroup message.
         * @function verify
         * @memberof chromeos_update_engine.DynamicPartitionGroup
         * @static
         * @param {Object.<string,*>} message Plain object to verify
         * @returns {string|null} `null` if valid, otherwise the reason why it is not
         */
        DynamicPartitionGroup.verify = function verify(message) {
            if (typeof message !== "object" || message === null)
                return "object expected";
            if (!$util.isString(message.name))
                return "name: string expected";
            if (message.size != null && message.hasOwnProperty("size"))
                if (!$util.isInteger(message.size) && !(message.size && $util.isInteger(message.size.low) && $util.isInteger(message.size.high)))
                    return "size: integer|Long expected";
            if (message.partitionNames != null && message.hasOwnProperty("partitionNames")) {
                if (!Array.isArray(message.partitionNames))
                    return "partitionNames: array expected";
                for (let i = 0; i < message.partitionNames.length; ++i)
                    if (!$util.isString(message.partitionNames[i]))
                        return "partitionNames: string[] expected";
            }
            return null;
        };

        /**
         * Creates a DynamicPartitionGroup message from a plain object. Also converts values to their respective internal types.
         * @function fromObject
         * @memberof chromeos_update_engine.DynamicPartitionGroup
         * @static
         * @param {Object.<string,*>} object Plain object
         * @returns {chromeos_update_engine.DynamicPartitionGroup} DynamicPartitionGroup
         */
        DynamicPartitionGroup.fromObject = function fromObject(object) {
            if (object instanceof $root.chromeos_update_engine.DynamicPartitionGroup)
                return object;
            let message = new $root.chromeos_update_engine.DynamicPartitionGroup();
            if (object.name != null)
                message.name = String(object.name);
            if (object.size != null)
                if ($util.Long)
                    (message.size = $util.Long.fromValue(object.size)).unsigned = true;
                else if (typeof object.size === "string")
                    message.size = parseInt(object.size, 10);
                else if (typeof object.size === "number")
                    message.size = object.size;
                else if (typeof object.size === "object")
                    message.size = new $util.LongBits(object.size.low >>> 0, object.size.high >>> 0).toNumber(true);
            if (object.partitionNames) {
                if (!Array.isArray(object.partitionNames))
                    throw TypeError(".chromeos_update_engine.DynamicPartitionGroup.partitionNames: array expected");
                message.partitionNames = [];
                for (let i = 0; i < object.partitionNames.length; ++i)
                    message.partitionNames[i] = String(object.partitionNames[i]);
            }
            return message;
        };

        /**
         * Creates a plain object from a DynamicPartitionGroup message. Also converts values to other types if specified.
         * @function toObject
         * @memberof chromeos_update_engine.DynamicPartitionGroup
         * @static
         * @param {chromeos_update_engine.DynamicPartitionGroup} message DynamicPartitionGroup
         * @param {$protobuf.IConversionOptions} [options] Conversion options
         * @returns {Object.<string,*>} Plain object
         */
        DynamicPartitionGroup.toObject = function toObject(message, options) {
            if (!options)
                options = {};
            let object = {};
            if (options.arrays || options.defaults)
                object.partitionNames = [];
            if (options.defaults) {
                object.name = "";
                if ($util.Long) {
                    let long = new $util.Long(0, 0, true);
                    object.size = options.longs === String ? long.toString() : options.longs === Number ? long.toNumber() : long;
                } else
                    object.size = options.longs === String ? "0" : 0;
            }
            if (message.name != null && message.hasOwnProperty("name"))
                object.name = message.name;
            if (message.size != null && message.hasOwnProperty("size"))
                if (typeof message.size === "number")
                    object.size = options.longs === String ? String(message.size) : message.size;
                else
                    object.size = options.longs === String ? $util.Long.prototype.toString.call(message.size) : options.longs === Number ? new $util.LongBits(message.size.low >>> 0, message.size.high >>> 0).toNumber(true) : message.size;
            if (message.partitionNames && message.partitionNames.length) {
                object.partitionNames = [];
                for (let j = 0; j < message.partitionNames.length; ++j)
                    object.partitionNames[j] = message.partitionNames[j];
            }
            return object;
        };

        /**
         * Converts this DynamicPartitionGroup to JSON.
         * @function toJSON
         * @memberof chromeos_update_engine.DynamicPartitionGroup
         * @instance
         * @returns {Object.<string,*>} JSON object
         */
        DynamicPartitionGroup.prototype.toJSON = function toJSON() {
            return this.constructor.toObject(this, $protobuf.util.toJSONOptions);
        };

        return DynamicPartitionGroup;
    })();

    chromeos_update_engine.DynamicPartitionMetadata = (function() {

        /**
         * Properties of a DynamicPartitionMetadata.
         * @memberof chromeos_update_engine
         * @interface IDynamicPartitionMetadata
         * @property {Array.<chromeos_update_engine.IDynamicPartitionGroup>|null} [groups] DynamicPartitionMetadata groups
         * @property {boolean|null} [snapshotEnabled] DynamicPartitionMetadata snapshotEnabled
         * @property {boolean|null} [vabcEnabled] DynamicPartitionMetadata vabcEnabled
         * @property {string|null} [vabcCompressionParam] DynamicPartitionMetadata vabcCompressionParam
         * @property {number|null} [cowVersion] DynamicPartitionMetadata cowVersion
         */

        /**
         * Constructs a new DynamicPartitionMetadata.
         * @memberof chromeos_update_engine
         * @classdesc Represents a DynamicPartitionMetadata.
         * @implements IDynamicPartitionMetadata
         * @constructor
         * @param {chromeos_update_engine.IDynamicPartitionMetadata=} [properties] Properties to set
         */
        function DynamicPartitionMetadata(properties) {
            this.groups = [];
            if (properties)
                for (let keys = Object.keys(properties), i = 0; i < keys.length; ++i)
                    if (properties[keys[i]] != null)
                        this[keys[i]] = properties[keys[i]];
        }

        /**
         * DynamicPartitionMetadata groups.
         * @member {Array.<chromeos_update_engine.IDynamicPartitionGroup>} groups
         * @memberof chromeos_update_engine.DynamicPartitionMetadata
         * @instance
         */
        DynamicPartitionMetadata.prototype.groups = $util.emptyArray;

        /**
         * DynamicPartitionMetadata snapshotEnabled.
         * @member {boolean} snapshotEnabled
         * @memberof chromeos_update_engine.DynamicPartitionMetadata
         * @instance
         */
        DynamicPartitionMetadata.prototype.snapshotEnabled = false;

        /**
         * DynamicPartitionMetadata vabcEnabled.
         * @member {boolean} vabcEnabled
         * @memberof chromeos_update_engine.DynamicPartitionMetadata
         * @instance
         */
        DynamicPartitionMetadata.prototype.vabcEnabled = false;

        /**
         * DynamicPartitionMetadata vabcCompressionParam.
         * @member {string} vabcCompressionParam
         * @memberof chromeos_update_engine.DynamicPartitionMetadata
         * @instance
         */
        DynamicPartitionMetadata.prototype.vabcCompressionParam = "";

        /**
         * DynamicPartitionMetadata cowVersion.
         * @member {number} cowVersion
         * @memberof chromeos_update_engine.DynamicPartitionMetadata
         * @instance
         */
        DynamicPartitionMetadata.prototype.cowVersion = 0;

        /**
         * Creates a new DynamicPartitionMetadata instance using the specified properties.
         * @function create
         * @memberof chromeos_update_engine.DynamicPartitionMetadata
         * @static
         * @param {chromeos_update_engine.IDynamicPartitionMetadata=} [properties] Properties to set
         * @returns {chromeos_update_engine.DynamicPartitionMetadata} DynamicPartitionMetadata instance
         */
        DynamicPartitionMetadata.create = function create(properties) {
            return new DynamicPartitionMetadata(properties);
        };

        /**
         * Encodes the specified DynamicPartitionMetadata message. Does not implicitly {@link chromeos_update_engine.DynamicPartitionMetadata.verify|verify} messages.
         * @function encode
         * @memberof chromeos_update_engine.DynamicPartitionMetadata
         * @static
         * @param {chromeos_update_engine.IDynamicPartitionMetadata} message DynamicPartitionMetadata message or plain object to encode
         * @param {$protobuf.Writer} [writer] Writer to encode to
         * @returns {$protobuf.Writer} Writer
         */
        DynamicPartitionMetadata.encode = function encode(message, writer) {
            if (!writer)
                writer = $Writer.create();
            if (message.groups != null && message.groups.length)
                for (let i = 0; i < message.groups.length; ++i)
                    $root.chromeos_update_engine.DynamicPartitionGroup.encode(message.groups[i], writer.uint32(/* id 1, wireType 2 =*/10).fork()).ldelim();
            if (message.snapshotEnabled != null && Object.hasOwnProperty.call(message, "snapshotEnabled"))
                writer.uint32(/* id 2, wireType 0 =*/16).bool(message.snapshotEnabled);
            if (message.vabcEnabled != null && Object.hasOwnProperty.call(message, "vabcEnabled"))
                writer.uint32(/* id 3, wireType 0 =*/24).bool(message.vabcEnabled);
            if (message.vabcCompressionParam != null && Object.hasOwnProperty.call(message, "vabcCompressionParam"))
                writer.uint32(/* id 4, wireType 2 =*/34).string(message.vabcCompressionParam);
            if (message.cowVersion != null && Object.hasOwnProperty.call(message, "cowVersion"))
                writer.uint32(/* id 5, wireType 0 =*/40).uint32(message.cowVersion);
            return writer;
        };

        /**
         * Encodes the specified DynamicPartitionMetadata message, length delimited. Does not implicitly {@link chromeos_update_engine.DynamicPartitionMetadata.verify|verify} messages.
         * @function encodeDelimited
         * @memberof chromeos_update_engine.DynamicPartitionMetadata
         * @static
         * @param {chromeos_update_engine.IDynamicPartitionMetadata} message DynamicPartitionMetadata message or plain object to encode
         * @param {$protobuf.Writer} [writer] Writer to encode to
         * @returns {$protobuf.Writer} Writer
         */
        DynamicPartitionMetadata.encodeDelimited = function encodeDelimited(message, writer) {
            return this.encode(message, writer).ldelim();
        };

        /**
         * Decodes a DynamicPartitionMetadata message from the specified reader or buffer.
         * @function decode
         * @memberof chromeos_update_engine.DynamicPartitionMetadata
         * @static
         * @param {$protobuf.Reader|Uint8Array} reader Reader or buffer to decode from
         * @param {number} [length] Message length if known beforehand
         * @returns {chromeos_update_engine.DynamicPartitionMetadata} DynamicPartitionMetadata
         * @throws {Error} If the payload is not a reader or valid buffer
         * @throws {$protobuf.util.ProtocolError} If required fields are missing
         */
        DynamicPartitionMetadata.decode = function decode(reader, length) {
            if (!(reader instanceof $Reader))
                reader = $Reader.create(reader);
            let end = length === undefined ? reader.len : reader.pos + length, message = new $root.chromeos_update_engine.DynamicPartitionMetadata();
            while (reader.pos < end) {
                let tag = reader.uint32();
                switch (tag >>> 3) {
                case 1:
                    if (!(message.groups && message.groups.length))
                        message.groups = [];
                    message.groups.push($root.chromeos_update_engine.DynamicPartitionGroup.decode(reader, reader.uint32()));
                    break;
                case 2:
                    message.snapshotEnabled = reader.bool();
                    break;
                case 3:
                    message.vabcEnabled = reader.bool();
                    break;
                case 4:
                    message.vabcCompressionParam = reader.string();
                    break;
                case 5:
                    message.cowVersion = reader.uint32();
                    break;
                default:
                    reader.skipType(tag & 7);
                    break;
                }
            }
            return message;
        };

        /**
         * Decodes a DynamicPartitionMetadata message from the specified reader or buffer, length delimited.
         * @function decodeDelimited
         * @memberof chromeos_update_engine.DynamicPartitionMetadata
         * @static
         * @param {$protobuf.Reader|Uint8Array} reader Reader or buffer to decode from
         * @returns {chromeos_update_engine.DynamicPartitionMetadata} DynamicPartitionMetadata
         * @throws {Error} If the payload is not a reader or valid buffer
         * @throws {$protobuf.util.ProtocolError} If required fields are missing
         */
        DynamicPartitionMetadata.decodeDelimited = function decodeDelimited(reader) {
            if (!(reader instanceof $Reader))
                reader = new $Reader(reader);
            return this.decode(reader, reader.uint32());
        };

        /**
         * Verifies a DynamicPartitionMetadata message.
         * @function verify
         * @memberof chromeos_update_engine.DynamicPartitionMetadata
         * @static
         * @param {Object.<string,*>} message Plain object to verify
         * @returns {string|null} `null` if valid, otherwise the reason why it is not
         */
        DynamicPartitionMetadata.verify = function verify(message) {
            if (typeof message !== "object" || message === null)
                return "object expected";
            if (message.groups != null && message.hasOwnProperty("groups")) {
                if (!Array.isArray(message.groups))
                    return "groups: array expected";
                for (let i = 0; i < message.groups.length; ++i) {
                    let error = $root.chromeos_update_engine.DynamicPartitionGroup.verify(message.groups[i]);
                    if (error)
                        return "groups." + error;
                }
            }
            if (message.snapshotEnabled != null && message.hasOwnProperty("snapshotEnabled"))
                if (typeof message.snapshotEnabled !== "boolean")
                    return "snapshotEnabled: boolean expected";
            if (message.vabcEnabled != null && message.hasOwnProperty("vabcEnabled"))
                if (typeof message.vabcEnabled !== "boolean")
                    return "vabcEnabled: boolean expected";
            if (message.vabcCompressionParam != null && message.hasOwnProperty("vabcCompressionParam"))
                if (!$util.isString(message.vabcCompressionParam))
                    return "vabcCompressionParam: string expected";
            if (message.cowVersion != null && message.hasOwnProperty("cowVersion"))
                if (!$util.isInteger(message.cowVersion))
                    return "cowVersion: integer expected";
            return null;
        };

        /**
         * Creates a DynamicPartitionMetadata message from a plain object. Also converts values to their respective internal types.
         * @function fromObject
         * @memberof chromeos_update_engine.DynamicPartitionMetadata
         * @static
         * @param {Object.<string,*>} object Plain object
         * @returns {chromeos_update_engine.DynamicPartitionMetadata} DynamicPartitionMetadata
         */
        DynamicPartitionMetadata.fromObject = function fromObject(object) {
            if (object instanceof $root.chromeos_update_engine.DynamicPartitionMetadata)
                return object;
            let message = new $root.chromeos_update_engine.DynamicPartitionMetadata();
            if (object.groups) {
                if (!Array.isArray(object.groups))
                    throw TypeError(".chromeos_update_engine.DynamicPartitionMetadata.groups: array expected");
                message.groups = [];
                for (let i = 0; i < object.groups.length; ++i) {
                    if (typeof object.groups[i] !== "object")
                        throw TypeError(".chromeos_update_engine.DynamicPartitionMetadata.groups: object expected");
                    message.groups[i] = $root.chromeos_update_engine.DynamicPartitionGroup.fromObject(object.groups[i]);
                }
            }
            if (object.snapshotEnabled != null)
                message.snapshotEnabled = Boolean(object.snapshotEnabled);
            if (object.vabcEnabled != null)
                message.vabcEnabled = Boolean(object.vabcEnabled);
            if (object.vabcCompressionParam != null)
                message.vabcCompressionParam = String(object.vabcCompressionParam);
            if (object.cowVersion != null)
                message.cowVersion = object.cowVersion >>> 0;
            return message;
        };

        /**
         * Creates a plain object from a DynamicPartitionMetadata message. Also converts values to other types if specified.
         * @function toObject
         * @memberof chromeos_update_engine.DynamicPartitionMetadata
         * @static
         * @param {chromeos_update_engine.DynamicPartitionMetadata} message DynamicPartitionMetadata
         * @param {$protobuf.IConversionOptions} [options] Conversion options
         * @returns {Object.<string,*>} Plain object
         */
        DynamicPartitionMetadata.toObject = function toObject(message, options) {
            if (!options)
                options = {};
            let object = {};
            if (options.arrays || options.defaults)
                object.groups = [];
            if (options.defaults) {
                object.snapshotEnabled = false;
                object.vabcEnabled = false;
                object.vabcCompressionParam = "";
                object.cowVersion = 0;
            }
            if (message.groups && message.groups.length) {
                object.groups = [];
                for (let j = 0; j < message.groups.length; ++j)
                    object.groups[j] = $root.chromeos_update_engine.DynamicPartitionGroup.toObject(message.groups[j], options);
            }
            if (message.snapshotEnabled != null && message.hasOwnProperty("snapshotEnabled"))
                object.snapshotEnabled = message.snapshotEnabled;
            if (message.vabcEnabled != null && message.hasOwnProperty("vabcEnabled"))
                object.vabcEnabled = message.vabcEnabled;
            if (message.vabcCompressionParam != null && message.hasOwnProperty("vabcCompressionParam"))
                object.vabcCompressionParam = message.vabcCompressionParam;
            if (message.cowVersion != null && message.hasOwnProperty("cowVersion"))
                object.cowVersion = message.cowVersion;
            return object;
        };

        /**
         * Converts this DynamicPartitionMetadata to JSON.
         * @function toJSON
         * @memberof chromeos_update_engine.DynamicPartitionMetadata
         * @instance
         * @returns {Object.<string,*>} JSON object
         */
        DynamicPartitionMetadata.prototype.toJSON = function toJSON() {
            return this.constructor.toObject(this, $protobuf.util.toJSONOptions);
        };

        return DynamicPartitionMetadata;
    })();

    chromeos_update_engine.ApexInfo = (function() {

        /**
         * Properties of an ApexInfo.
         * @memberof chromeos_update_engine
         * @interface IApexInfo
         * @property {string|null} [packageName] ApexInfo packageName
         * @property {number|Long|null} [version] ApexInfo version
         * @property {boolean|null} [isCompressed] ApexInfo isCompressed
         * @property {number|Long|null} [decompressedSize] ApexInfo decompressedSize
         */

        /**
         * Constructs a new ApexInfo.
         * @memberof chromeos_update_engine
         * @classdesc Represents an ApexInfo.
         * @implements IApexInfo
         * @constructor
         * @param {chromeos_update_engine.IApexInfo=} [properties] Properties to set
         */
        function ApexInfo(properties) {
            if (properties)
                for (let keys = Object.keys(properties), i = 0; i < keys.length; ++i)
                    if (properties[keys[i]] != null)
                        this[keys[i]] = properties[keys[i]];
        }

        /**
         * ApexInfo packageName.
         * @member {string} packageName
         * @memberof chromeos_update_engine.ApexInfo
         * @instance
         */
        ApexInfo.prototype.packageName = "";

        /**
         * ApexInfo version.
         * @member {number|Long} version
         * @memberof chromeos_update_engine.ApexInfo
         * @instance
         */
        ApexInfo.prototype.version = $util.Long ? $util.Long.fromBits(0,0,false) : 0;

        /**
         * ApexInfo isCompressed.
         * @member {boolean} isCompressed
         * @memberof chromeos_update_engine.ApexInfo
         * @instance
         */
        ApexInfo.prototype.isCompressed = false;

        /**
         * ApexInfo decompressedSize.
         * @member {number|Long} decompressedSize
         * @memberof chromeos_update_engine.ApexInfo
         * @instance
         */
        ApexInfo.prototype.decompressedSize = $util.Long ? $util.Long.fromBits(0,0,false) : 0;

        /**
         * Creates a new ApexInfo instance using the specified properties.
         * @function create
         * @memberof chromeos_update_engine.ApexInfo
         * @static
         * @param {chromeos_update_engine.IApexInfo=} [properties] Properties to set
         * @returns {chromeos_update_engine.ApexInfo} ApexInfo instance
         */
        ApexInfo.create = function create(properties) {
            return new ApexInfo(properties);
        };

        /**
         * Encodes the specified ApexInfo message. Does not implicitly {@link chromeos_update_engine.ApexInfo.verify|verify} messages.
         * @function encode
         * @memberof chromeos_update_engine.ApexInfo
         * @static
         * @param {chromeos_update_engine.IApexInfo} message ApexInfo message or plain object to encode
         * @param {$protobuf.Writer} [writer] Writer to encode to
         * @returns {$protobuf.Writer} Writer
         */
        ApexInfo.encode = function encode(message, writer) {
            if (!writer)
                writer = $Writer.create();
            if (message.packageName != null && Object.hasOwnProperty.call(message, "packageName"))
                writer.uint32(/* id 1, wireType 2 =*/10).string(message.packageName);
            if (message.version != null && Object.hasOwnProperty.call(message, "version"))
                writer.uint32(/* id 2, wireType 0 =*/16).int64(message.version);
            if (message.isCompressed != null && Object.hasOwnProperty.call(message, "isCompressed"))
                writer.uint32(/* id 3, wireType 0 =*/24).bool(message.isCompressed);
            if (message.decompressedSize != null && Object.hasOwnProperty.call(message, "decompressedSize"))
                writer.uint32(/* id 4, wireType 0 =*/32).int64(message.decompressedSize);
            return writer;
        };

        /**
         * Encodes the specified ApexInfo message, length delimited. Does not implicitly {@link chromeos_update_engine.ApexInfo.verify|verify} messages.
         * @function encodeDelimited
         * @memberof chromeos_update_engine.ApexInfo
         * @static
         * @param {chromeos_update_engine.IApexInfo} message ApexInfo message or plain object to encode
         * @param {$protobuf.Writer} [writer] Writer to encode to
         * @returns {$protobuf.Writer} Writer
         */
        ApexInfo.encodeDelimited = function encodeDelimited(message, writer) {
            return this.encode(message, writer).ldelim();
        };

        /**
         * Decodes an ApexInfo message from the specified reader or buffer.
         * @function decode
         * @memberof chromeos_update_engine.ApexInfo
         * @static
         * @param {$protobuf.Reader|Uint8Array} reader Reader or buffer to decode from
         * @param {number} [length] Message length if known beforehand
         * @returns {chromeos_update_engine.ApexInfo} ApexInfo
         * @throws {Error} If the payload is not a reader or valid buffer
         * @throws {$protobuf.util.ProtocolError} If required fields are missing
         */
        ApexInfo.decode = function decode(reader, length) {
            if (!(reader instanceof $Reader))
                reader = $Reader.create(reader);
            let end = length === undefined ? reader.len : reader.pos + length, message = new $root.chromeos_update_engine.ApexInfo();
            while (reader.pos < end) {
                let tag = reader.uint32();
                switch (tag >>> 3) {
                case 1:
                    message.packageName = reader.string();
                    break;
                case 2:
                    message.version = reader.int64();
                    break;
                case 3:
                    message.isCompressed = reader.bool();
                    break;
                case 4:
                    message.decompressedSize = reader.int64();
                    break;
                default:
                    reader.skipType(tag & 7);
                    break;
                }
            }
            return message;
        };

        /**
         * Decodes an ApexInfo message from the specified reader or buffer, length delimited.
         * @function decodeDelimited
         * @memberof chromeos_update_engine.ApexInfo
         * @static
         * @param {$protobuf.Reader|Uint8Array} reader Reader or buffer to decode from
         * @returns {chromeos_update_engine.ApexInfo} ApexInfo
         * @throws {Error} If the payload is not a reader or valid buffer
         * @throws {$protobuf.util.ProtocolError} If required fields are missing
         */
        ApexInfo.decodeDelimited = function decodeDelimited(reader) {
            if (!(reader instanceof $Reader))
                reader = new $Reader(reader);
            return this.decode(reader, reader.uint32());
        };

        /**
         * Verifies an ApexInfo message.
         * @function verify
         * @memberof chromeos_update_engine.ApexInfo
         * @static
         * @param {Object.<string,*>} message Plain object to verify
         * @returns {string|null} `null` if valid, otherwise the reason why it is not
         */
        ApexInfo.verify = function verify(message) {
            if (typeof message !== "object" || message === null)
                return "object expected";
            if (message.packageName != null && message.hasOwnProperty("packageName"))
                if (!$util.isString(message.packageName))
                    return "packageName: string expected";
            if (message.version != null && message.hasOwnProperty("version"))
                if (!$util.isInteger(message.version) && !(message.version && $util.isInteger(message.version.low) && $util.isInteger(message.version.high)))
                    return "version: integer|Long expected";
            if (message.isCompressed != null && message.hasOwnProperty("isCompressed"))
                if (typeof message.isCompressed !== "boolean")
                    return "isCompressed: boolean expected";
            if (message.decompressedSize != null && message.hasOwnProperty("decompressedSize"))
                if (!$util.isInteger(message.decompressedSize) && !(message.decompressedSize && $util.isInteger(message.decompressedSize.low) && $util.isInteger(message.decompressedSize.high)))
                    return "decompressedSize: integer|Long expected";
            return null;
        };

        /**
         * Creates an ApexInfo message from a plain object. Also converts values to their respective internal types.
         * @function fromObject
         * @memberof chromeos_update_engine.ApexInfo
         * @static
         * @param {Object.<string,*>} object Plain object
         * @returns {chromeos_update_engine.ApexInfo} ApexInfo
         */
        ApexInfo.fromObject = function fromObject(object) {
            if (object instanceof $root.chromeos_update_engine.ApexInfo)
                return object;
            let message = new $root.chromeos_update_engine.ApexInfo();
            if (object.packageName != null)
                message.packageName = String(object.packageName);
            if (object.version != null)
                if ($util.Long)
                    (message.version = $util.Long.fromValue(object.version)).unsigned = false;
                else if (typeof object.version === "string")
                    message.version = parseInt(object.version, 10);
                else if (typeof object.version === "number")
                    message.version = object.version;
                else if (typeof object.version === "object")
                    message.version = new $util.LongBits(object.version.low >>> 0, object.version.high >>> 0).toNumber();
            if (object.isCompressed != null)
                message.isCompressed = Boolean(object.isCompressed);
            if (object.decompressedSize != null)
                if ($util.Long)
                    (message.decompressedSize = $util.Long.fromValue(object.decompressedSize)).unsigned = false;
                else if (typeof object.decompressedSize === "string")
                    message.decompressedSize = parseInt(object.decompressedSize, 10);
                else if (typeof object.decompressedSize === "number")
                    message.decompressedSize = object.decompressedSize;
                else if (typeof object.decompressedSize === "object")
                    message.decompressedSize = new $util.LongBits(object.decompressedSize.low >>> 0, object.decompressedSize.high >>> 0).toNumber();
            return message;
        };

        /**
         * Creates a plain object from an ApexInfo message. Also converts values to other types if specified.
         * @function toObject
         * @memberof chromeos_update_engine.ApexInfo
         * @static
         * @param {chromeos_update_engine.ApexInfo} message ApexInfo
         * @param {$protobuf.IConversionOptions} [options] Conversion options
         * @returns {Object.<string,*>} Plain object
         */
        ApexInfo.toObject = function toObject(message, options) {
            if (!options)
                options = {};
            let object = {};
            if (options.defaults) {
                object.packageName = "";
                if ($util.Long) {
                    let long = new $util.Long(0, 0, false);
                    object.version = options.longs === String ? long.toString() : options.longs === Number ? long.toNumber() : long;
                } else
                    object.version = options.longs === String ? "0" : 0;
                object.isCompressed = false;
                if ($util.Long) {
                    let long = new $util.Long(0, 0, false);
                    object.decompressedSize = options.longs === String ? long.toString() : options.longs === Number ? long.toNumber() : long;
                } else
                    object.decompressedSize = options.longs === String ? "0" : 0;
            }
            if (message.packageName != null && message.hasOwnProperty("packageName"))
                object.packageName = message.packageName;
            if (message.version != null && message.hasOwnProperty("version"))
                if (typeof message.version === "number")
                    object.version = options.longs === String ? String(message.version) : message.version;
                else
                    object.version = options.longs === String ? $util.Long.prototype.toString.call(message.version) : options.longs === Number ? new $util.LongBits(message.version.low >>> 0, message.version.high >>> 0).toNumber() : message.version;
            if (message.isCompressed != null && message.hasOwnProperty("isCompressed"))
                object.isCompressed = message.isCompressed;
            if (message.decompressedSize != null && message.hasOwnProperty("decompressedSize"))
                if (typeof message.decompressedSize === "number")
                    object.decompressedSize = options.longs === String ? String(message.decompressedSize) : message.decompressedSize;
                else
                    object.decompressedSize = options.longs === String ? $util.Long.prototype.toString.call(message.decompressedSize) : options.longs === Number ? new $util.LongBits(message.decompressedSize.low >>> 0, message.decompressedSize.high >>> 0).toNumber() : message.decompressedSize;
            return object;
        };

        /**
         * Converts this ApexInfo to JSON.
         * @function toJSON
         * @memberof chromeos_update_engine.ApexInfo
         * @instance
         * @returns {Object.<string,*>} JSON object
         */
        ApexInfo.prototype.toJSON = function toJSON() {
            return this.constructor.toObject(this, $protobuf.util.toJSONOptions);
        };

        return ApexInfo;
    })();

    chromeos_update_engine.ApexMetadata = (function() {

        /**
         * Properties of an ApexMetadata.
         * @memberof chromeos_update_engine
         * @interface IApexMetadata
         * @property {Array.<chromeos_update_engine.IApexInfo>|null} [apexInfo] ApexMetadata apexInfo
         */

        /**
         * Constructs a new ApexMetadata.
         * @memberof chromeos_update_engine
         * @classdesc Represents an ApexMetadata.
         * @implements IApexMetadata
         * @constructor
         * @param {chromeos_update_engine.IApexMetadata=} [properties] Properties to set
         */
        function ApexMetadata(properties) {
            this.apexInfo = [];
            if (properties)
                for (let keys = Object.keys(properties), i = 0; i < keys.length; ++i)
                    if (properties[keys[i]] != null)
                        this[keys[i]] = properties[keys[i]];
        }

        /**
         * ApexMetadata apexInfo.
         * @member {Array.<chromeos_update_engine.IApexInfo>} apexInfo
         * @memberof chromeos_update_engine.ApexMetadata
         * @instance
         */
        ApexMetadata.prototype.apexInfo = $util.emptyArray;

        /**
         * Creates a new ApexMetadata instance using the specified properties.
         * @function create
         * @memberof chromeos_update_engine.ApexMetadata
         * @static
         * @param {chromeos_update_engine.IApexMetadata=} [properties] Properties to set
         * @returns {chromeos_update_engine.ApexMetadata} ApexMetadata instance
         */
        ApexMetadata.create = function create(properties) {
            return new ApexMetadata(properties);
        };

        /**
         * Encodes the specified ApexMetadata message. Does not implicitly {@link chromeos_update_engine.ApexMetadata.verify|verify} messages.
         * @function encode
         * @memberof chromeos_update_engine.ApexMetadata
         * @static
         * @param {chromeos_update_engine.IApexMetadata} message ApexMetadata message or plain object to encode
         * @param {$protobuf.Writer} [writer] Writer to encode to
         * @returns {$protobuf.Writer} Writer
         */
        ApexMetadata.encode = function encode(message, writer) {
            if (!writer)
                writer = $Writer.create();
            if (message.apexInfo != null && message.apexInfo.length)
                for (let i = 0; i < message.apexInfo.length; ++i)
                    $root.chromeos_update_engine.ApexInfo.encode(message.apexInfo[i], writer.uint32(/* id 1, wireType 2 =*/10).fork()).ldelim();
            return writer;
        };

        /**
         * Encodes the specified ApexMetadata message, length delimited. Does not implicitly {@link chromeos_update_engine.ApexMetadata.verify|verify} messages.
         * @function encodeDelimited
         * @memberof chromeos_update_engine.ApexMetadata
         * @static
         * @param {chromeos_update_engine.IApexMetadata} message ApexMetadata message or plain object to encode
         * @param {$protobuf.Writer} [writer] Writer to encode to
         * @returns {$protobuf.Writer} Writer
         */
        ApexMetadata.encodeDelimited = function encodeDelimited(message, writer) {
            return this.encode(message, writer).ldelim();
        };

        /**
         * Decodes an ApexMetadata message from the specified reader or buffer.
         * @function decode
         * @memberof chromeos_update_engine.ApexMetadata
         * @static
         * @param {$protobuf.Reader|Uint8Array} reader Reader or buffer to decode from
         * @param {number} [length] Message length if known beforehand
         * @returns {chromeos_update_engine.ApexMetadata} ApexMetadata
         * @throws {Error} If the payload is not a reader or valid buffer
         * @throws {$protobuf.util.ProtocolError} If required fields are missing
         */
        ApexMetadata.decode = function decode(reader, length) {
            if (!(reader instanceof $Reader))
                reader = $Reader.create(reader);
            let end = length === undefined ? reader.len : reader.pos + length, message = new $root.chromeos_update_engine.ApexMetadata();
            while (reader.pos < end) {
                let tag = reader.uint32();
                switch (tag >>> 3) {
                case 1:
                    if (!(message.apexInfo && message.apexInfo.length))
                        message.apexInfo = [];
                    message.apexInfo.push($root.chromeos_update_engine.ApexInfo.decode(reader, reader.uint32()));
                    break;
                default:
                    reader.skipType(tag & 7);
                    break;
                }
            }
            return message;
        };

        /**
         * Decodes an ApexMetadata message from the specified reader or buffer, length delimited.
         * @function decodeDelimited
         * @memberof chromeos_update_engine.ApexMetadata
         * @static
         * @param {$protobuf.Reader|Uint8Array} reader Reader or buffer to decode from
         * @returns {chromeos_update_engine.ApexMetadata} ApexMetadata
         * @throws {Error} If the payload is not a reader or valid buffer
         * @throws {$protobuf.util.ProtocolError} If required fields are missing
         */
        ApexMetadata.decodeDelimited = function decodeDelimited(reader) {
            if (!(reader instanceof $Reader))
                reader = new $Reader(reader);
            return this.decode(reader, reader.uint32());
        };

        /**
         * Verifies an ApexMetadata message.
         * @function verify
         * @memberof chromeos_update_engine.ApexMetadata
         * @static
         * @param {Object.<string,*>} message Plain object to verify
         * @returns {string|null} `null` if valid, otherwise the reason why it is not
         */
        ApexMetadata.verify = function verify(message) {
            if (typeof message !== "object" || message === null)
                return "object expected";
            if (message.apexInfo != null && message.hasOwnProperty("apexInfo")) {
                if (!Array.isArray(message.apexInfo))
                    return "apexInfo: array expected";
                for (let i = 0; i < message.apexInfo.length; ++i) {
                    let error = $root.chromeos_update_engine.ApexInfo.verify(message.apexInfo[i]);
                    if (error)
                        return "apexInfo." + error;
                }
            }
            return null;
        };

        /**
         * Creates an ApexMetadata message from a plain object. Also converts values to their respective internal types.
         * @function fromObject
         * @memberof chromeos_update_engine.ApexMetadata
         * @static
         * @param {Object.<string,*>} object Plain object
         * @returns {chromeos_update_engine.ApexMetadata} ApexMetadata
         */
        ApexMetadata.fromObject = function fromObject(object) {
            if (object instanceof $root.chromeos_update_engine.ApexMetadata)
                return object;
            let message = new $root.chromeos_update_engine.ApexMetadata();
            if (object.apexInfo) {
                if (!Array.isArray(object.apexInfo))
                    throw TypeError(".chromeos_update_engine.ApexMetadata.apexInfo: array expected");
                message.apexInfo = [];
                for (let i = 0; i < object.apexInfo.length; ++i) {
                    if (typeof object.apexInfo[i] !== "object")
                        throw TypeError(".chromeos_update_engine.ApexMetadata.apexInfo: object expected");
                    message.apexInfo[i] = $root.chromeos_update_engine.ApexInfo.fromObject(object.apexInfo[i]);
                }
            }
            return message;
        };

        /**
         * Creates a plain object from an ApexMetadata message. Also converts values to other types if specified.
         * @function toObject
         * @memberof chromeos_update_engine.ApexMetadata
         * @static
         * @param {chromeos_update_engine.ApexMetadata} message ApexMetadata
         * @param {$protobuf.IConversionOptions} [options] Conversion options
         * @returns {Object.<string,*>} Plain object
         */
        ApexMetadata.toObject = function toObject(message, options) {
            if (!options)
                options = {};
            let object = {};
            if (options.arrays || options.defaults)
                object.apexInfo = [];
            if (message.apexInfo && message.apexInfo.length) {
                object.apexInfo = [];
                for (let j = 0; j < message.apexInfo.length; ++j)
                    object.apexInfo[j] = $root.chromeos_update_engine.ApexInfo.toObject(message.apexInfo[j], options);
            }
            return object;
        };

        /**
         * Converts this ApexMetadata to JSON.
         * @function toJSON
         * @memberof chromeos_update_engine.ApexMetadata
         * @instance
         * @returns {Object.<string,*>} JSON object
         */
        ApexMetadata.prototype.toJSON = function toJSON() {
            return this.constructor.toObject(this, $protobuf.util.toJSONOptions);
        };

        return ApexMetadata;
    })();

    chromeos_update_engine.DeltaArchiveManifest = (function() {

        /**
         * Properties of a DeltaArchiveManifest.
         * @memberof chromeos_update_engine
         * @interface IDeltaArchiveManifest
         * @property {Array.<chromeos_update_engine.IInstallOperation>|null} [installOperations] DeltaArchiveManifest installOperations
         * @property {Array.<chromeos_update_engine.IInstallOperation>|null} [kernelInstallOperations] DeltaArchiveManifest kernelInstallOperations
         * @property {number|null} [blockSize] DeltaArchiveManifest blockSize
         * @property {number|Long|null} [signaturesOffset] DeltaArchiveManifest signaturesOffset
         * @property {number|Long|null} [signaturesSize] DeltaArchiveManifest signaturesSize
         * @property {chromeos_update_engine.IPartitionInfo|null} [oldKernelInfo] DeltaArchiveManifest oldKernelInfo
         * @property {chromeos_update_engine.IPartitionInfo|null} [newKernelInfo] DeltaArchiveManifest newKernelInfo
         * @property {chromeos_update_engine.IPartitionInfo|null} [oldRootfsInfo] DeltaArchiveManifest oldRootfsInfo
         * @property {chromeos_update_engine.IPartitionInfo|null} [newRootfsInfo] DeltaArchiveManifest newRootfsInfo
         * @property {chromeos_update_engine.IImageInfo|null} [oldImageInfo] DeltaArchiveManifest oldImageInfo
         * @property {chromeos_update_engine.IImageInfo|null} [newImageInfo] DeltaArchiveManifest newImageInfo
         * @property {number|null} [minorVersion] DeltaArchiveManifest minorVersion
         * @property {Array.<chromeos_update_engine.IPartitionUpdate>|null} [partitions] DeltaArchiveManifest partitions
         * @property {number|Long|null} [maxTimestamp] DeltaArchiveManifest maxTimestamp
         * @property {chromeos_update_engine.IDynamicPartitionMetadata|null} [dynamicPartitionMetadata] DeltaArchiveManifest dynamicPartitionMetadata
         * @property {boolean|null} [partialUpdate] DeltaArchiveManifest partialUpdate
         * @property {Array.<chromeos_update_engine.IApexInfo>|null} [apexInfo] DeltaArchiveManifest apexInfo
         */

        /**
         * Constructs a new DeltaArchiveManifest.
         * @memberof chromeos_update_engine
         * @classdesc Represents a DeltaArchiveManifest.
         * @implements IDeltaArchiveManifest
         * @constructor
         * @param {chromeos_update_engine.IDeltaArchiveManifest=} [properties] Properties to set
         */
        function DeltaArchiveManifest(properties) {
            this.installOperations = [];
            this.kernelInstallOperations = [];
            this.partitions = [];
            this.apexInfo = [];
            if (properties)
                for (let keys = Object.keys(properties), i = 0; i < keys.length; ++i)
                    if (properties[keys[i]] != null)
                        this[keys[i]] = properties[keys[i]];
        }

        /**
         * DeltaArchiveManifest installOperations.
         * @member {Array.<chromeos_update_engine.IInstallOperation>} installOperations
         * @memberof chromeos_update_engine.DeltaArchiveManifest
         * @instance
         */
        DeltaArchiveManifest.prototype.installOperations = $util.emptyArray;

        /**
         * DeltaArchiveManifest kernelInstallOperations.
         * @member {Array.<chromeos_update_engine.IInstallOperation>} kernelInstallOperations
         * @memberof chromeos_update_engine.DeltaArchiveManifest
         * @instance
         */
        DeltaArchiveManifest.prototype.kernelInstallOperations = $util.emptyArray;

        /**
         * DeltaArchiveManifest blockSize.
         * @member {number} blockSize
         * @memberof chromeos_update_engine.DeltaArchiveManifest
         * @instance
         */
        DeltaArchiveManifest.prototype.blockSize = 4096;

        /**
         * DeltaArchiveManifest signaturesOffset.
         * @member {number|Long} signaturesOffset
         * @memberof chromeos_update_engine.DeltaArchiveManifest
         * @instance
         */
        DeltaArchiveManifest.prototype.signaturesOffset = $util.Long ? $util.Long.fromBits(0,0,true) : 0;

        /**
         * DeltaArchiveManifest signaturesSize.
         * @member {number|Long} signaturesSize
         * @memberof chromeos_update_engine.DeltaArchiveManifest
         * @instance
         */
        DeltaArchiveManifest.prototype.signaturesSize = $util.Long ? $util.Long.fromBits(0,0,true) : 0;

        /**
         * DeltaArchiveManifest oldKernelInfo.
         * @member {chromeos_update_engine.IPartitionInfo|null|undefined} oldKernelInfo
         * @memberof chromeos_update_engine.DeltaArchiveManifest
         * @instance
         */
        DeltaArchiveManifest.prototype.oldKernelInfo = null;

        /**
         * DeltaArchiveManifest newKernelInfo.
         * @member {chromeos_update_engine.IPartitionInfo|null|undefined} newKernelInfo
         * @memberof chromeos_update_engine.DeltaArchiveManifest
         * @instance
         */
        DeltaArchiveManifest.prototype.newKernelInfo = null;

        /**
         * DeltaArchiveManifest oldRootfsInfo.
         * @member {chromeos_update_engine.IPartitionInfo|null|undefined} oldRootfsInfo
         * @memberof chromeos_update_engine.DeltaArchiveManifest
         * @instance
         */
        DeltaArchiveManifest.prototype.oldRootfsInfo = null;

        /**
         * DeltaArchiveManifest newRootfsInfo.
         * @member {chromeos_update_engine.IPartitionInfo|null|undefined} newRootfsInfo
         * @memberof chromeos_update_engine.DeltaArchiveManifest
         * @instance
         */
        DeltaArchiveManifest.prototype.newRootfsInfo = null;

        /**
         * DeltaArchiveManifest oldImageInfo.
         * @member {chromeos_update_engine.IImageInfo|null|undefined} oldImageInfo
         * @memberof chromeos_update_engine.DeltaArchiveManifest
         * @instance
         */
        DeltaArchiveManifest.prototype.oldImageInfo = null;

        /**
         * DeltaArchiveManifest newImageInfo.
         * @member {chromeos_update_engine.IImageInfo|null|undefined} newImageInfo
         * @memberof chromeos_update_engine.DeltaArchiveManifest
         * @instance
         */
        DeltaArchiveManifest.prototype.newImageInfo = null;

        /**
         * DeltaArchiveManifest minorVersion.
         * @member {number} minorVersion
         * @memberof chromeos_update_engine.DeltaArchiveManifest
         * @instance
         */
        DeltaArchiveManifest.prototype.minorVersion = 0;

        /**
         * DeltaArchiveManifest partitions.
         * @member {Array.<chromeos_update_engine.IPartitionUpdate>} partitions
         * @memberof chromeos_update_engine.DeltaArchiveManifest
         * @instance
         */
        DeltaArchiveManifest.prototype.partitions = $util.emptyArray;

        /**
         * DeltaArchiveManifest maxTimestamp.
         * @member {number|Long} maxTimestamp
         * @memberof chromeos_update_engine.DeltaArchiveManifest
         * @instance
         */
        DeltaArchiveManifest.prototype.maxTimestamp = $util.Long ? $util.Long.fromBits(0,0,false) : 0;

        /**
         * DeltaArchiveManifest dynamicPartitionMetadata.
         * @member {chromeos_update_engine.IDynamicPartitionMetadata|null|undefined} dynamicPartitionMetadata
         * @memberof chromeos_update_engine.DeltaArchiveManifest
         * @instance
         */
        DeltaArchiveManifest.prototype.dynamicPartitionMetadata = null;

        /**
         * DeltaArchiveManifest partialUpdate.
         * @member {boolean} partialUpdate
         * @memberof chromeos_update_engine.DeltaArchiveManifest
         * @instance
         */
        DeltaArchiveManifest.prototype.partialUpdate = false;

        /**
         * DeltaArchiveManifest apexInfo.
         * @member {Array.<chromeos_update_engine.IApexInfo>} apexInfo
         * @memberof chromeos_update_engine.DeltaArchiveManifest
         * @instance
         */
        DeltaArchiveManifest.prototype.apexInfo = $util.emptyArray;

        /**
         * Creates a new DeltaArchiveManifest instance using the specified properties.
         * @function create
         * @memberof chromeos_update_engine.DeltaArchiveManifest
         * @static
         * @param {chromeos_update_engine.IDeltaArchiveManifest=} [properties] Properties to set
         * @returns {chromeos_update_engine.DeltaArchiveManifest} DeltaArchiveManifest instance
         */
        DeltaArchiveManifest.create = function create(properties) {
            return new DeltaArchiveManifest(properties);
        };

        /**
         * Encodes the specified DeltaArchiveManifest message. Does not implicitly {@link chromeos_update_engine.DeltaArchiveManifest.verify|verify} messages.
         * @function encode
         * @memberof chromeos_update_engine.DeltaArchiveManifest
         * @static
         * @param {chromeos_update_engine.IDeltaArchiveManifest} message DeltaArchiveManifest message or plain object to encode
         * @param {$protobuf.Writer} [writer] Writer to encode to
         * @returns {$protobuf.Writer} Writer
         */
        DeltaArchiveManifest.encode = function encode(message, writer) {
            if (!writer)
                writer = $Writer.create();
            if (message.installOperations != null && message.installOperations.length)
                for (let i = 0; i < message.installOperations.length; ++i)
                    $root.chromeos_update_engine.InstallOperation.encode(message.installOperations[i], writer.uint32(/* id 1, wireType 2 =*/10).fork()).ldelim();
            if (message.kernelInstallOperations != null && message.kernelInstallOperations.length)
                for (let i = 0; i < message.kernelInstallOperations.length; ++i)
                    $root.chromeos_update_engine.InstallOperation.encode(message.kernelInstallOperations[i], writer.uint32(/* id 2, wireType 2 =*/18).fork()).ldelim();
            if (message.blockSize != null && Object.hasOwnProperty.call(message, "blockSize"))
                writer.uint32(/* id 3, wireType 0 =*/24).uint32(message.blockSize);
            if (message.signaturesOffset != null && Object.hasOwnProperty.call(message, "signaturesOffset"))
                writer.uint32(/* id 4, wireType 0 =*/32).uint64(message.signaturesOffset);
            if (message.signaturesSize != null && Object.hasOwnProperty.call(message, "signaturesSize"))
                writer.uint32(/* id 5, wireType 0 =*/40).uint64(message.signaturesSize);
            if (message.oldKernelInfo != null && Object.hasOwnProperty.call(message, "oldKernelInfo"))
                $root.chromeos_update_engine.PartitionInfo.encode(message.oldKernelInfo, writer.uint32(/* id 6, wireType 2 =*/50).fork()).ldelim();
            if (message.newKernelInfo != null && Object.hasOwnProperty.call(message, "newKernelInfo"))
                $root.chromeos_update_engine.PartitionInfo.encode(message.newKernelInfo, writer.uint32(/* id 7, wireType 2 =*/58).fork()).ldelim();
            if (message.oldRootfsInfo != null && Object.hasOwnProperty.call(message, "oldRootfsInfo"))
                $root.chromeos_update_engine.PartitionInfo.encode(message.oldRootfsInfo, writer.uint32(/* id 8, wireType 2 =*/66).fork()).ldelim();
            if (message.newRootfsInfo != null && Object.hasOwnProperty.call(message, "newRootfsInfo"))
                $root.chromeos_update_engine.PartitionInfo.encode(message.newRootfsInfo, writer.uint32(/* id 9, wireType 2 =*/74).fork()).ldelim();
            if (message.oldImageInfo != null && Object.hasOwnProperty.call(message, "oldImageInfo"))
                $root.chromeos_update_engine.ImageInfo.encode(message.oldImageInfo, writer.uint32(/* id 10, wireType 2 =*/82).fork()).ldelim();
            if (message.newImageInfo != null && Object.hasOwnProperty.call(message, "newImageInfo"))
                $root.chromeos_update_engine.ImageInfo.encode(message.newImageInfo, writer.uint32(/* id 11, wireType 2 =*/90).fork()).ldelim();
            if (message.minorVersion != null && Object.hasOwnProperty.call(message, "minorVersion"))
                writer.uint32(/* id 12, wireType 0 =*/96).uint32(message.minorVersion);
            if (message.partitions != null && message.partitions.length)
                for (let i = 0; i < message.partitions.length; ++i)
                    $root.chromeos_update_engine.PartitionUpdate.encode(message.partitions[i], writer.uint32(/* id 13, wireType 2 =*/106).fork()).ldelim();
            if (message.maxTimestamp != null && Object.hasOwnProperty.call(message, "maxTimestamp"))
                writer.uint32(/* id 14, wireType 0 =*/112).int64(message.maxTimestamp);
            if (message.dynamicPartitionMetadata != null && Object.hasOwnProperty.call(message, "dynamicPartitionMetadata"))
                $root.chromeos_update_engine.DynamicPartitionMetadata.encode(message.dynamicPartitionMetadata, writer.uint32(/* id 15, wireType 2 =*/122).fork()).ldelim();
            if (message.partialUpdate != null && Object.hasOwnProperty.call(message, "partialUpdate"))
                writer.uint32(/* id 16, wireType 0 =*/128).bool(message.partialUpdate);
            if (message.apexInfo != null && message.apexInfo.length)
                for (let i = 0; i < message.apexInfo.length; ++i)
                    $root.chromeos_update_engine.ApexInfo.encode(message.apexInfo[i], writer.uint32(/* id 17, wireType 2 =*/138).fork()).ldelim();
            return writer;
        };

        /**
         * Encodes the specified DeltaArchiveManifest message, length delimited. Does not implicitly {@link chromeos_update_engine.DeltaArchiveManifest.verify|verify} messages.
         * @function encodeDelimited
         * @memberof chromeos_update_engine.DeltaArchiveManifest
         * @static
         * @param {chromeos_update_engine.IDeltaArchiveManifest} message DeltaArchiveManifest message or plain object to encode
         * @param {$protobuf.Writer} [writer] Writer to encode to
         * @returns {$protobuf.Writer} Writer
         */
        DeltaArchiveManifest.encodeDelimited = function encodeDelimited(message, writer) {
            return this.encode(message, writer).ldelim();
        };

        /**
         * Decodes a DeltaArchiveManifest message from the specified reader or buffer.
         * @function decode
         * @memberof chromeos_update_engine.DeltaArchiveManifest
         * @static
         * @param {$protobuf.Reader|Uint8Array} reader Reader or buffer to decode from
         * @param {number} [length] Message length if known beforehand
         * @returns {chromeos_update_engine.DeltaArchiveManifest} DeltaArchiveManifest
         * @throws {Error} If the payload is not a reader or valid buffer
         * @throws {$protobuf.util.ProtocolError} If required fields are missing
         */
        DeltaArchiveManifest.decode = function decode(reader, length) {
            if (!(reader instanceof $Reader))
                reader = $Reader.create(reader);
            let end = length === undefined ? reader.len : reader.pos + length, message = new $root.chromeos_update_engine.DeltaArchiveManifest();
            while (reader.pos < end) {
                let tag = reader.uint32();
                switch (tag >>> 3) {
                case 1:
                    if (!(message.installOperations && message.installOperations.length))
                        message.installOperations = [];
                    message.installOperations.push($root.chromeos_update_engine.InstallOperation.decode(reader, reader.uint32()));
                    break;
                case 2:
                    if (!(message.kernelInstallOperations && message.kernelInstallOperations.length))
                        message.kernelInstallOperations = [];
                    message.kernelInstallOperations.push($root.chromeos_update_engine.InstallOperation.decode(reader, reader.uint32()));
                    break;
                case 3:
                    message.blockSize = reader.uint32();
                    break;
                case 4:
                    message.signaturesOffset = reader.uint64();
                    break;
                case 5:
                    message.signaturesSize = reader.uint64();
                    break;
                case 6:
                    message.oldKernelInfo = $root.chromeos_update_engine.PartitionInfo.decode(reader, reader.uint32());
                    break;
                case 7:
                    message.newKernelInfo = $root.chromeos_update_engine.PartitionInfo.decode(reader, reader.uint32());
                    break;
                case 8:
                    message.oldRootfsInfo = $root.chromeos_update_engine.PartitionInfo.decode(reader, reader.uint32());
                    break;
                case 9:
                    message.newRootfsInfo = $root.chromeos_update_engine.PartitionInfo.decode(reader, reader.uint32());
                    break;
                case 10:
                    message.oldImageInfo = $root.chromeos_update_engine.ImageInfo.decode(reader, reader.uint32());
                    break;
                case 11:
                    message.newImageInfo = $root.chromeos_update_engine.ImageInfo.decode(reader, reader.uint32());
                    break;
                case 12:
                    message.minorVersion = reader.uint32();
                    break;
                case 13:
                    if (!(message.partitions && message.partitions.length))
                        message.partitions = [];
                    message.partitions.push($root.chromeos_update_engine.PartitionUpdate.decode(reader, reader.uint32()));
                    break;
                case 14:
                    message.maxTimestamp = reader.int64();
                    break;
                case 15:
                    message.dynamicPartitionMetadata = $root.chromeos_update_engine.DynamicPartitionMetadata.decode(reader, reader.uint32());
                    break;
                case 16:
                    message.partialUpdate = reader.bool();
                    break;
                case 17:
                    if (!(message.apexInfo && message.apexInfo.length))
                        message.apexInfo = [];
                    message.apexInfo.push($root.chromeos_update_engine.ApexInfo.decode(reader, reader.uint32()));
                    break;
                default:
                    reader.skipType(tag & 7);
                    break;
                }
            }
            return message;
        };

        /**
         * Decodes a DeltaArchiveManifest message from the specified reader or buffer, length delimited.
         * @function decodeDelimited
         * @memberof chromeos_update_engine.DeltaArchiveManifest
         * @static
         * @param {$protobuf.Reader|Uint8Array} reader Reader or buffer to decode from
         * @returns {chromeos_update_engine.DeltaArchiveManifest} DeltaArchiveManifest
         * @throws {Error} If the payload is not a reader or valid buffer
         * @throws {$protobuf.util.ProtocolError} If required fields are missing
         */
        DeltaArchiveManifest.decodeDelimited = function decodeDelimited(reader) {
            if (!(reader instanceof $Reader))
                reader = new $Reader(reader);
            return this.decode(reader, reader.uint32());
        };

        /**
         * Verifies a DeltaArchiveManifest message.
         * @function verify
         * @memberof chromeos_update_engine.DeltaArchiveManifest
         * @static
         * @param {Object.<string,*>} message Plain object to verify
         * @returns {string|null} `null` if valid, otherwise the reason why it is not
         */
        DeltaArchiveManifest.verify = function verify(message) {
            if (typeof message !== "object" || message === null)
                return "object expected";
            if (message.installOperations != null && message.hasOwnProperty("installOperations")) {
                if (!Array.isArray(message.installOperations))
                    return "installOperations: array expected";
                for (let i = 0; i < message.installOperations.length; ++i) {
                    let error = $root.chromeos_update_engine.InstallOperation.verify(message.installOperations[i]);
                    if (error)
                        return "installOperations." + error;
                }
            }
            if (message.kernelInstallOperations != null && message.hasOwnProperty("kernelInstallOperations")) {
                if (!Array.isArray(message.kernelInstallOperations))
                    return "kernelInstallOperations: array expected";
                for (let i = 0; i < message.kernelInstallOperations.length; ++i) {
                    let error = $root.chromeos_update_engine.InstallOperation.verify(message.kernelInstallOperations[i]);
                    if (error)
                        return "kernelInstallOperations." + error;
                }
            }
            if (message.blockSize != null && message.hasOwnProperty("blockSize"))
                if (!$util.isInteger(message.blockSize))
                    return "blockSize: integer expected";
            if (message.signaturesOffset != null && message.hasOwnProperty("signaturesOffset"))
                if (!$util.isInteger(message.signaturesOffset) && !(message.signaturesOffset && $util.isInteger(message.signaturesOffset.low) && $util.isInteger(message.signaturesOffset.high)))
                    return "signaturesOffset: integer|Long expected";
            if (message.signaturesSize != null && message.hasOwnProperty("signaturesSize"))
                if (!$util.isInteger(message.signaturesSize) && !(message.signaturesSize && $util.isInteger(message.signaturesSize.low) && $util.isInteger(message.signaturesSize.high)))
                    return "signaturesSize: integer|Long expected";
            if (message.oldKernelInfo != null && message.hasOwnProperty("oldKernelInfo")) {
                let error = $root.chromeos_update_engine.PartitionInfo.verify(message.oldKernelInfo);
                if (error)
                    return "oldKernelInfo." + error;
            }
            if (message.newKernelInfo != null && message.hasOwnProperty("newKernelInfo")) {
                let error = $root.chromeos_update_engine.PartitionInfo.verify(message.newKernelInfo);
                if (error)
                    return "newKernelInfo." + error;
            }
            if (message.oldRootfsInfo != null && message.hasOwnProperty("oldRootfsInfo")) {
                let error = $root.chromeos_update_engine.PartitionInfo.verify(message.oldRootfsInfo);
                if (error)
                    return "oldRootfsInfo." + error;
            }
            if (message.newRootfsInfo != null && message.hasOwnProperty("newRootfsInfo")) {
                let error = $root.chromeos_update_engine.PartitionInfo.verify(message.newRootfsInfo);
                if (error)
                    return "newRootfsInfo." + error;
            }
            if (message.oldImageInfo != null && message.hasOwnProperty("oldImageInfo")) {
                let error = $root.chromeos_update_engine.ImageInfo.verify(message.oldImageInfo);
                if (error)
                    return "oldImageInfo." + error;
            }
            if (message.newImageInfo != null && message.hasOwnProperty("newImageInfo")) {
                let error = $root.chromeos_update_engine.ImageInfo.verify(message.newImageInfo);
                if (error)
                    return "newImageInfo." + error;
            }
            if (message.minorVersion != null && message.hasOwnProperty("minorVersion"))
                if (!$util.isInteger(message.minorVersion))
                    return "minorVersion: integer expected";
            if (message.partitions != null && message.hasOwnProperty("partitions")) {
                if (!Array.isArray(message.partitions))
                    return "partitions: array expected";
                for (let i = 0; i < message.partitions.length; ++i) {
                    let error = $root.chromeos_update_engine.PartitionUpdate.verify(message.partitions[i]);
                    if (error)
                        return "partitions." + error;
                }
            }
            if (message.maxTimestamp != null && message.hasOwnProperty("maxTimestamp"))
                if (!$util.isInteger(message.maxTimestamp) && !(message.maxTimestamp && $util.isInteger(message.maxTimestamp.low) && $util.isInteger(message.maxTimestamp.high)))
                    return "maxTimestamp: integer|Long expected";
            if (message.dynamicPartitionMetadata != null && message.hasOwnProperty("dynamicPartitionMetadata")) {
                let error = $root.chromeos_update_engine.DynamicPartitionMetadata.verify(message.dynamicPartitionMetadata);
                if (error)
                    return "dynamicPartitionMetadata." + error;
            }
            if (message.partialUpdate != null && message.hasOwnProperty("partialUpdate"))
                if (typeof message.partialUpdate !== "boolean")
                    return "partialUpdate: boolean expected";
            if (message.apexInfo != null && message.hasOwnProperty("apexInfo")) {
                if (!Array.isArray(message.apexInfo))
                    return "apexInfo: array expected";
                for (let i = 0; i < message.apexInfo.length; ++i) {
                    let error = $root.chromeos_update_engine.ApexInfo.verify(message.apexInfo[i]);
                    if (error)
                        return "apexInfo." + error;
                }
            }
            return null;
        };

        /**
         * Creates a DeltaArchiveManifest message from a plain object. Also converts values to their respective internal types.
         * @function fromObject
         * @memberof chromeos_update_engine.DeltaArchiveManifest
         * @static
         * @param {Object.<string,*>} object Plain object
         * @returns {chromeos_update_engine.DeltaArchiveManifest} DeltaArchiveManifest
         */
        DeltaArchiveManifest.fromObject = function fromObject(object) {
            if (object instanceof $root.chromeos_update_engine.DeltaArchiveManifest)
                return object;
            let message = new $root.chromeos_update_engine.DeltaArchiveManifest();
            if (object.installOperations) {
                if (!Array.isArray(object.installOperations))
                    throw TypeError(".chromeos_update_engine.DeltaArchiveManifest.installOperations: array expected");
                message.installOperations = [];
                for (let i = 0; i < object.installOperations.length; ++i) {
                    if (typeof object.installOperations[i] !== "object")
                        throw TypeError(".chromeos_update_engine.DeltaArchiveManifest.installOperations: object expected");
                    message.installOperations[i] = $root.chromeos_update_engine.InstallOperation.fromObject(object.installOperations[i]);
                }
            }
            if (object.kernelInstallOperations) {
                if (!Array.isArray(object.kernelInstallOperations))
                    throw TypeError(".chromeos_update_engine.DeltaArchiveManifest.kernelInstallOperations: array expected");
                message.kernelInstallOperations = [];
                for (let i = 0; i < object.kernelInstallOperations.length; ++i) {
                    if (typeof object.kernelInstallOperations[i] !== "object")
                        throw TypeError(".chromeos_update_engine.DeltaArchiveManifest.kernelInstallOperations: object expected");
                    message.kernelInstallOperations[i] = $root.chromeos_update_engine.InstallOperation.fromObject(object.kernelInstallOperations[i]);
                }
            }
            if (object.blockSize != null)
                message.blockSize = object.blockSize >>> 0;
            if (object.signaturesOffset != null)
                if ($util.Long)
                    (message.signaturesOffset = $util.Long.fromValue(object.signaturesOffset)).unsigned = true;
                else if (typeof object.signaturesOffset === "string")
                    message.signaturesOffset = parseInt(object.signaturesOffset, 10);
                else if (typeof object.signaturesOffset === "number")
                    message.signaturesOffset = object.signaturesOffset;
                else if (typeof object.signaturesOffset === "object")
                    message.signaturesOffset = new $util.LongBits(object.signaturesOffset.low >>> 0, object.signaturesOffset.high >>> 0).toNumber(true);
            if (object.signaturesSize != null)
                if ($util.Long)
                    (message.signaturesSize = $util.Long.fromValue(object.signaturesSize)).unsigned = true;
                else if (typeof object.signaturesSize === "string")
                    message.signaturesSize = parseInt(object.signaturesSize, 10);
                else if (typeof object.signaturesSize === "number")
                    message.signaturesSize = object.signaturesSize;
                else if (typeof object.signaturesSize === "object")
                    message.signaturesSize = new $util.LongBits(object.signaturesSize.low >>> 0, object.signaturesSize.high >>> 0).toNumber(true);
            if (object.oldKernelInfo != null) {
                if (typeof object.oldKernelInfo !== "object")
                    throw TypeError(".chromeos_update_engine.DeltaArchiveManifest.oldKernelInfo: object expected");
                message.oldKernelInfo = $root.chromeos_update_engine.PartitionInfo.fromObject(object.oldKernelInfo);
            }
            if (object.newKernelInfo != null) {
                if (typeof object.newKernelInfo !== "object")
                    throw TypeError(".chromeos_update_engine.DeltaArchiveManifest.newKernelInfo: object expected");
                message.newKernelInfo = $root.chromeos_update_engine.PartitionInfo.fromObject(object.newKernelInfo);
            }
            if (object.oldRootfsInfo != null) {
                if (typeof object.oldRootfsInfo !== "object")
                    throw TypeError(".chromeos_update_engine.DeltaArchiveManifest.oldRootfsInfo: object expected");
                message.oldRootfsInfo = $root.chromeos_update_engine.PartitionInfo.fromObject(object.oldRootfsInfo);
            }
            if (object.newRootfsInfo != null) {
                if (typeof object.newRootfsInfo !== "object")
                    throw TypeError(".chromeos_update_engine.DeltaArchiveManifest.newRootfsInfo: object expected");
                message.newRootfsInfo = $root.chromeos_update_engine.PartitionInfo.fromObject(object.newRootfsInfo);
            }
            if (object.oldImageInfo != null) {
                if (typeof object.oldImageInfo !== "object")
                    throw TypeError(".chromeos_update_engine.DeltaArchiveManifest.oldImageInfo: object expected");
                message.oldImageInfo = $root.chromeos_update_engine.ImageInfo.fromObject(object.oldImageInfo);
            }
            if (object.newImageInfo != null) {
                if (typeof object.newImageInfo !== "object")
                    throw TypeError(".chromeos_update_engine.DeltaArchiveManifest.newImageInfo: object expected");
                message.newImageInfo = $root.chromeos_update_engine.ImageInfo.fromObject(object.newImageInfo);
            }
            if (object.minorVersion != null)
                message.minorVersion = object.minorVersion >>> 0;
            if (object.partitions) {
                if (!Array.isArray(object.partitions))
                    throw TypeError(".chromeos_update_engine.DeltaArchiveManifest.partitions: array expected");
                message.partitions = [];
                for (let i = 0; i < object.partitions.length; ++i) {
                    if (typeof object.partitions[i] !== "object")
                        throw TypeError(".chromeos_update_engine.DeltaArchiveManifest.partitions: object expected");
                    message.partitions[i] = $root.chromeos_update_engine.PartitionUpdate.fromObject(object.partitions[i]);
                }
            }
            if (object.maxTimestamp != null)
                if ($util.Long)
                    (message.maxTimestamp = $util.Long.fromValue(object.maxTimestamp)).unsigned = false;
                else if (typeof object.maxTimestamp === "string")
                    message.maxTimestamp = parseInt(object.maxTimestamp, 10);
                else if (typeof object.maxTimestamp === "number")
                    message.maxTimestamp = object.maxTimestamp;
                else if (typeof object.maxTimestamp === "object")
                    message.maxTimestamp = new $util.LongBits(object.maxTimestamp.low >>> 0, object.maxTimestamp.high >>> 0).toNumber();
            if (object.dynamicPartitionMetadata != null) {
                if (typeof object.dynamicPartitionMetadata !== "object")
                    throw TypeError(".chromeos_update_engine.DeltaArchiveManifest.dynamicPartitionMetadata: object expected");
                message.dynamicPartitionMetadata = $root.chromeos_update_engine.DynamicPartitionMetadata.fromObject(object.dynamicPartitionMetadata);
            }
            if (object.partialUpdate != null)
                message.partialUpdate = Boolean(object.partialUpdate);
            if (object.apexInfo) {
                if (!Array.isArray(object.apexInfo))
                    throw TypeError(".chromeos_update_engine.DeltaArchiveManifest.apexInfo: array expected");
                message.apexInfo = [];
                for (let i = 0; i < object.apexInfo.length; ++i) {
                    if (typeof object.apexInfo[i] !== "object")
                        throw TypeError(".chromeos_update_engine.DeltaArchiveManifest.apexInfo: object expected");
                    message.apexInfo[i] = $root.chromeos_update_engine.ApexInfo.fromObject(object.apexInfo[i]);
                }
            }
            return message;
        };

        /**
         * Creates a plain object from a DeltaArchiveManifest message. Also converts values to other types if specified.
         * @function toObject
         * @memberof chromeos_update_engine.DeltaArchiveManifest
         * @static
         * @param {chromeos_update_engine.DeltaArchiveManifest} message DeltaArchiveManifest
         * @param {$protobuf.IConversionOptions} [options] Conversion options
         * @returns {Object.<string,*>} Plain object
         */
        DeltaArchiveManifest.toObject = function toObject(message, options) {
            if (!options)
                options = {};
            let object = {};
            if (options.arrays || options.defaults) {
                object.installOperations = [];
                object.kernelInstallOperations = [];
                object.partitions = [];
                object.apexInfo = [];
            }
            if (options.defaults) {
                object.blockSize = 4096;
                if ($util.Long) {
                    let long = new $util.Long(0, 0, true);
                    object.signaturesOffset = options.longs === String ? long.toString() : options.longs === Number ? long.toNumber() : long;
                } else
                    object.signaturesOffset = options.longs === String ? "0" : 0;
                if ($util.Long) {
                    let long = new $util.Long(0, 0, true);
                    object.signaturesSize = options.longs === String ? long.toString() : options.longs === Number ? long.toNumber() : long;
                } else
                    object.signaturesSize = options.longs === String ? "0" : 0;
                object.oldKernelInfo = null;
                object.newKernelInfo = null;
                object.oldRootfsInfo = null;
                object.newRootfsInfo = null;
                object.oldImageInfo = null;
                object.newImageInfo = null;
                object.minorVersion = 0;
                if ($util.Long) {
                    let long = new $util.Long(0, 0, false);
                    object.maxTimestamp = options.longs === String ? long.toString() : options.longs === Number ? long.toNumber() : long;
                } else
                    object.maxTimestamp = options.longs === String ? "0" : 0;
                object.dynamicPartitionMetadata = null;
                object.partialUpdate = false;
            }
            if (message.installOperations && message.installOperations.length) {
                object.installOperations = [];
                for (let j = 0; j < message.installOperations.length; ++j)
                    object.installOperations[j] = $root.chromeos_update_engine.InstallOperation.toObject(message.installOperations[j], options);
            }
            if (message.kernelInstallOperations && message.kernelInstallOperations.length) {
                object.kernelInstallOperations = [];
                for (let j = 0; j < message.kernelInstallOperations.length; ++j)
                    object.kernelInstallOperations[j] = $root.chromeos_update_engine.InstallOperation.toObject(message.kernelInstallOperations[j], options);
            }
            if (message.blockSize != null && message.hasOwnProperty("blockSize"))
                object.blockSize = message.blockSize;
            if (message.signaturesOffset != null && message.hasOwnProperty("signaturesOffset"))
                if (typeof message.signaturesOffset === "number")
                    object.signaturesOffset = options.longs === String ? String(message.signaturesOffset) : message.signaturesOffset;
                else
                    object.signaturesOffset = options.longs === String ? $util.Long.prototype.toString.call(message.signaturesOffset) : options.longs === Number ? new $util.LongBits(message.signaturesOffset.low >>> 0, message.signaturesOffset.high >>> 0).toNumber(true) : message.signaturesOffset;
            if (message.signaturesSize != null && message.hasOwnProperty("signaturesSize"))
                if (typeof message.signaturesSize === "number")
                    object.signaturesSize = options.longs === String ? String(message.signaturesSize) : message.signaturesSize;
                else
                    object.signaturesSize = options.longs === String ? $util.Long.prototype.toString.call(message.signaturesSize) : options.longs === Number ? new $util.LongBits(message.signaturesSize.low >>> 0, message.signaturesSize.high >>> 0).toNumber(true) : message.signaturesSize;
            if (message.oldKernelInfo != null && message.hasOwnProperty("oldKernelInfo"))
                object.oldKernelInfo = $root.chromeos_update_engine.PartitionInfo.toObject(message.oldKernelInfo, options);
            if (message.newKernelInfo != null && message.hasOwnProperty("newKernelInfo"))
                object.newKernelInfo = $root.chromeos_update_engine.PartitionInfo.toObject(message.newKernelInfo, options);
            if (message.oldRootfsInfo != null && message.hasOwnProperty("oldRootfsInfo"))
                object.oldRootfsInfo = $root.chromeos_update_engine.PartitionInfo.toObject(message.oldRootfsInfo, options);
            if (message.newRootfsInfo != null && message.hasOwnProperty("newRootfsInfo"))
                object.newRootfsInfo = $root.chromeos_update_engine.PartitionInfo.toObject(message.newRootfsInfo, options);
            if (message.oldImageInfo != null && message.hasOwnProperty("oldImageInfo"))
                object.oldImageInfo = $root.chromeos_update_engine.ImageInfo.toObject(message.oldImageInfo, options);
            if (message.newImageInfo != null && message.hasOwnProperty("newImageInfo"))
                object.newImageInfo = $root.chromeos_update_engine.ImageInfo.toObject(message.newImageInfo, options);
            if (message.minorVersion != null && message.hasOwnProperty("minorVersion"))
                object.minorVersion = message.minorVersion;
            if (message.partitions && message.partitions.length) {
                object.partitions = [];
                for (let j = 0; j < message.partitions.length; ++j)
                    object.partitions[j] = $root.chromeos_update_engine.PartitionUpdate.toObject(message.partitions[j], options);
            }
            if (message.maxTimestamp != null && message.hasOwnProperty("maxTimestamp"))
                if (typeof message.maxTimestamp === "number")
                    object.maxTimestamp = options.longs === String ? String(message.maxTimestamp) : message.maxTimestamp;
                else
                    object.maxTimestamp = options.longs === String ? $util.Long.prototype.toString.call(message.maxTimestamp) : options.longs === Number ? new $util.LongBits(message.maxTimestamp.low >>> 0, message.maxTimestamp.high >>> 0).toNumber() : message.maxTimestamp;
            if (message.dynamicPartitionMetadata != null && message.hasOwnProperty("dynamicPartitionMetadata"))
                object.dynamicPartitionMetadata = $root.chromeos_update_engine.DynamicPartitionMetadata.toObject(message.dynamicPartitionMetadata, options);
            if (message.partialUpdate != null && message.hasOwnProperty("partialUpdate"))
                object.partialUpdate = message.partialUpdate;
            if (message.apexInfo && message.apexInfo.length) {
                object.apexInfo = [];
                for (let j = 0; j < message.apexInfo.length; ++j)
                    object.apexInfo[j] = $root.chromeos_update_engine.ApexInfo.toObject(message.apexInfo[j], options);
            }
            return object;
        };

        /**
         * Converts this DeltaArchiveManifest to JSON.
         * @function toJSON
         * @memberof chromeos_update_engine.DeltaArchiveManifest
         * @instance
         * @returns {Object.<string,*>} JSON object
         */
        DeltaArchiveManifest.prototype.toJSON = function toJSON() {
            return this.constructor.toObject(this, $protobuf.util.toJSONOptions);
        };

        return DeltaArchiveManifest;
    })();

    return chromeos_update_engine;
})();

export { $root as default };
