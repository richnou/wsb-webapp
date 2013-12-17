package com.idyria.osi.wsb.webapp.http.connector.websocket

import com.idyria.osi.wsb.core.network.protocols.ProtocolHandler
import java.nio.ByteBuffer
import com.idyria.osi.wsb.core.network.NetworkContext
import scala.collection.JavaConversions._
import com.idyria.osi.wsb.core.message.Message
import java.nio.ByteOrder
import com.idyria.osi.tea.logging.TLogSource

class WebsocketProtocolhandler(var localContext: NetworkContext) extends ProtocolHandler[ByteBuffer](localContext) with TLogSource {

  val bytesDescription = RegisterFile(WebsocketProtocolhandler.protocolDescription)
  val sendDescription = RegisterFile(WebsocketProtocolhandler.protocolDescription)

  /**
   * Count of remaining datas, if 0, we are processing normal message, not waiting for payload to be completed
   */
  var remainingDatas = 0

  var payloadLength: Int = 0

  var maskingKey: Option[Array[Int]] = None

  var readDatas: ByteBuffer = _

  def receive(buffer: ByteBuffer): Boolean = {

    logFine(s"Received some Websocket datas, with ordering: ${buffer.order().toString()} (native: ${java.nio.ByteOrder.nativeOrder()}), capacity: " + buffer.remaining())

    //buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN)

    remainingDatas match {

      // Message Mode
      //--------------------
      case 0 =>

        // Take the first 4 Bytes and analyse using header
        //---------------------------
        var header = bytesDescription.register("header")
        header.value = buffer.getInt()

        //header.explainMemory

        logFine("After header remaining: " + buffer.remaining())

        // !!
        // !! The ordering of following code is mandatory (because of buffer sequential reading)
        // !!

        // Get Payload Length
        //-------------------
        payloadLength = header.field("PayloadLen").value match {

          // Last two bytes of Header Bytes are not used as payload length, so rewind by 2
          case l if (l < 126) =>

            buffer.position(buffer.position - 2)
            l.toInt

          // Take last two bytes of header as length
          case l if (l == 126) => (header.field("ExtraPayloadLen").value).toInt

          // Use ExtraPayloadLen and Extended PayLoad len as one register to concatenate the bytes
          case _ =>

            var extendedLength = bytesDescription.register("ExtendedPayloadLen")

            //-- Rewind 2 bytes in buffer, and read a Long 
            buffer.position(buffer.position() - 2)
            extendedLength.value = buffer.getLong()

            extendedLength.value.data.toInt
        }

        logFine(s"After payload length ($payloadLength): " + buffer.remaining())

        // Get Masking Key Octets
        //-----------------------
        maskingKey = header.field("MASK").value match {

          //-- Read 4 bytes of masking key
          case 1 =>
            var key = buffer.getInt

            logFine(s"Masking key: " + key.toHexString)

            var keyOctets = Array((key >> 0) & 0xFF, (key >>> 8) & 0xFF, (key >>> 16) & 0xFF, (key >>> 24) & 0xFF)

            //-- Reverse Bytes if ordering of system and buffer differs
            buffer.order() match {
              case order if (order != java.nio.ByteOrder.nativeOrder()) => Some(keyOctets.reverse)
              case _ => Some(keyOctets)
            }

          case 0 =>

            None
        }

        logFine("After key length: " + buffer.remaining())

        // Get PayLoad
        //-----------------

        //-- Prepare output Buffer
        this.readDatas = ByteBuffer.allocate(payloadLength)

        //-- Read Data up to buffer end

        remainingDatas = payloadLength - buffer.remaining()
        this.readDatas.put(buffer)

        // Handle data type
        //--------------------------
        header.field("OPCODE").value match {

          case WebsocketProtocolhandler.OpCode.TEXT_FRAME   =>

          //println(s"Found Text Data of length: " + payloadLength)
          //println(s"Countent: " + new String(payLoad.map { v => v.toChar }))

          case WebsocketProtocolhandler.OpCode.PING         =>
          case WebsocketProtocolhandler.OpCode.PONG         =>
          case WebsocketProtocolhandler.OpCode.BINARY_FRAME =>

        }

      // Data Receive mode
      //---------------------------
      case _ =>

        //-- Read
        remainingDatas = payloadLength - buffer.remaining()
        this.readDatas.put(buffer)

    }

    // Finish Datas
    //------------------
    remainingDatas match {
      case 0 =>

        //var array = new Array[Byte](payloadLength)
        // readDatas.flip()
        var array = readDatas.array

        logFine(s"Decoding length: " + array.length)

        var payLoad = maskingKey match {

          //-- Decode Through key XOR buffer
          case Some(keyOctets) =>

            // Map to char to avoid Signed Byte logic, and reverse the bytes which are in the wrong order
            // var intArray = array.map { _.toInt match { case v if (v < 0) => ~(v); case v => v } }

            //-- Reverse Bytes if ordering of system and buffer differs
            var intArray = buffer.order() match {
              case order if (order != java.nio.ByteOrder.nativeOrder()) => array.map { v => (v & 0xFF) }
              case _ => array.map { v => (v & 0xFF) }
            }

            WebsocketProtocolhandler.unmask(intArray, keyOctets)

          //-- Don't decode
          case None =>

            buffer.order() match {
              case order if (order != java.nio.ByteOrder.nativeOrder()) => array.reverse.map { v => (v & 0xFF) }
              case _ => array.map { v => (v & 0xFF) }
            }

        }

        //-- Stack results
        //println(s"Countent: " + new String(payLoad.map { v => v.toChar }))
        this.availableDatas.push(ByteBuffer.wrap(payLoad.map { v => v.toByte }))

        logFine(s"Stacked: " + new String(this.availableDatas.head.array()))

        payloadLength = 0
      case _ =>
    }
    var res = (remainingDatas == 0)

    // println(s"Remaining datas to wait for: " + remainingDatas + "//" + res)

    res

  }

  def send(buffer: ByteBuffer, nc: NetworkContext): ByteBuffer = {

    logFine(s"Sending Websocket for length of: " + buffer.remaining)

    //-- Prepare Header
    //-------------------------
    var head = sendDescription.register("header")
    head.value = 0

    head.field("FIN").value = 1

    //-- Send text
    head.field("OPCODE").value = 0x1

    //-- Length
    var lengthExtraBytes = buffer.remaining() match {

      // Length on 7 bits
      case payloadLength if (payloadLength < 126) =>

        head.field("PayloadLen").value = payloadLength
        0

      // Length on 16 bits
      case payloadLength if (payloadLength < Math.pow(2, 16)) =>

        head.field("PayloadLen").value = 126
        head.field("ExtraPayloadLen").value = payloadLength
        2

      // Otherwise 64 bits
      case _ =>

        head.field("PayloadLen").value = 127
        sendDescription.register("ExtendedPayloadLen").value = payloadLength

        8

    }

    //-- Header length: 2 bytes + eventual extra payloadlength
    var totalLength = 2 + lengthExtraBytes + buffer.remaining()

    // Write Message to new Buffer
    //----------------
    var outBuffer = ByteBuffer.allocate(totalLength)
    //outBuffer.order(ByteOrder.BIG_ENDIAN)

    //head.explainMemory

    logFine(s"Head byte 0 (payload +length: ${(head.value >>> 8).toBinaryString}")
    outBuffer.put(((head.value >>> 24) & 0xFF).toByte)

    outBuffer.put(((head.value >>> 16) & 0xFF).toByte)

    //-- First output length because of endianness
    lengthExtraBytes match {
      case 2 =>
        outBuffer.put(((head.value >>> 8) & 0xFF).toByte)
        outBuffer.put(((head.value >>> 0) & 0xFF).toByte)
      case 8 =>
        outBuffer.putLong(sendDescription.register("ExtendedPayloadLen").value)
      case _ =>
    }

    outBuffer.put(buffer)

    //ByteBuffer.wrap(outBuffer.array().reverse)

    // outBuffer.flip()
    //outBuffer
  }

}
object WebsocketProtocolhandler {

  val protocolDescription = <RegisterFile>
                              <!-- 4 Bytes -->
                              <Register name="header">
                                <Field name="ExtraPayloadLen" width="16"></Field>
                                <Field name="PayloadLen" width="7"></Field>
                                <Field name="MASK" width="1"></Field>
                                <Field name="OPCODE" width="4"></Field>
                                <Field name="RSV3" width="1"></Field>
                                <Field name="RSV2" width="1"></Field>
                                <Field name="RSV1" width="1"></Field>
                                <Field name="FIN" width="1"></Field>
                              </Register>
                              <!-- 8 bytes : Combines with 6 read bytes and remaining two of header -->
                              <Register name="ExtendedPayloadLen">
                                <Field name="ExtraPayloadLen" width="16"></Field>
                                <Field name="ExtendedPayloadLen" width="48"></Field>
                              </Register>
                              <Register name="MaskingKey">
                                <Field name="MaskingKey" width="32"></Field>
                              </Register>
                            </RegisterFile>

  object OpCode extends Enumeration {
    type OpCode = Int

    val CONTINUATION = 0x0
    val TEXT_FRAME = 0x1
    val BINARY_FRAME = 0x2
    val RSV3 = 0x3
    val RSV4 = 0x4
    val RSV5 = 0x5
    val RSV6 = 0x6
    val RSV7 = 0x7
    val CLOSE = 0x8
    val PING = 0x9
    val PONG = 0xA
  }

  def unmask(bytes: Array[Int], keyOctets: Array[Int]): Array[Int] = {

    // Map to char to avoid Signed Byte logic, and reverse the bytes which are in the wrong order
    // var intArray = array.map { _.toInt match { case v if (v < 0) => ~(v); case v => v } }
    //var intArray = bytes.map { v => (v & 0xFF) }
    //var intArray = bytes

    // Decode
    // j                   = i MOD 4
    // transformed-octet-i = original-octet-i XOR masking-key-octet-j
    /*for (i <- 0 to (intArray.length - 1)) {

      var keyOctet = keyOctets(i % 4)

      //println(s"Doing XOR of ${intArray(i).toHexString} // ${intArray(i)} with ${keyOctet.toHexString}")

      //-- XOR
      intArray(i) = (intArray(i) ^ keyOctet)

      // println(s"Doing XOR of ${intArray(i).toHexString} with ${keyOctet.toHexString}")

    }*/

    var resArray = for (i <- 0 to (bytes.length - 1)) yield (bytes(i) ^ keyOctets(i % 4))

    resArray.toArray

    // intArray

  }

}