package io.netflow.lib

import java.net.{ InetAddress, InetSocketAddress }
import java.util.UUID

import com.datastax.driver.core.utils.UUIDs
import io.netty.buffer.ByteBuf
import net.liftweb.json.JsonDSL._
import net.liftweb.json._
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

trait Flow[T] {
  def version: String
  def length: Int
  def sender: InetSocketAddress
  def senderIP = sender.getAddress.getHostAddress
  def senderPort = sender.getPort
  def json: String
  def jsonExtra: JObject = JObject(Nil)
}

/**
 * Unified interface for UDP Packets generated by NetFlow and sFlow.
 */
trait FlowPacket {
  def id: UUID
  def version: String
  def length: Int
  def sender: InetSocketAddress
  def senderIP = sender.getAddress.getHostAddress
  def senderPort = sender.getPort
  def timestamp: DateTime
  def count: Int
  def flows: List[Flow[_]]
  def persist: Unit
}

/**
 * Unified interface for accounting statistics (InetAddress and Networks) generated by NetFlow.
 */
trait NetFlowData[T] extends Flow[T] {
  this: T with Flow[T] =>

  def srcPort: Int
  def dstPort: Int
  def srcAS: Option[Int]
  def dstAS: Option[Int]
  def srcAddress: InetAddress
  def dstAddress: InetAddress
  def nextHop: Option[InetAddress]
  def pkts: Long
  def bytes: Long
  def proto: Int
  def tos: Int
  def tcpflags: Int
  def start: DateTime
  def stop: DateTime
  def duration: Long = stop.getMillis - start.getMillis

  private def srcAddressIP = srcAddress.getHostAddress
  private def dstAddressIP = dstAddress.getHostAddress
  private def nextHopIP = nextHop.map(_.getHostAddress)

  lazy val json = Serialization.write {
    ("flowVersion" -> version) ~
      ("flowSender" -> (senderIP + ":" + senderPort)) ~
      ("srcPort" -> srcPort) ~
      ("dstPort" -> dstPort) ~
      ("srcAddress" -> srcAddressIP) ~
      ("dstAddress" -> dstAddressIP) ~
      ("srcAS" -> srcAS) ~
      ("dstAS" -> dstAS) ~
      ("nextHop" -> nextHopIP) ~
      ("proto" -> proto) ~
      ("tos" -> tos) ~
      ("pkts" -> pkts) ~
      ("bytes" -> bytes) ~
      ("start" -> start.toString(ISODateTimeFormat.dateTime())) ~
      ("stop" -> stop.toString(ISODateTimeFormat.dateTime())) ~
      ("duration" -> duration) ~
      ("tcpFlags" -> tcpflags) ~ jsonExtra
  }

  protected def stringExtra = ""
  override def toString = "%s reported by %s:%s containing %s:%s%s -> %s -> %s:%s%s Proto %s - ToS %s - %s pkts - %s bytes %s".format(
    version, senderIP, senderPort, srcAddress.getHostAddress, srcPort, srcAS.filter(_ == 0).map(" (" + _ + ")").getOrElse(""),
    nextHopIP.getOrElse("direct"), dstAddress.getHostAddress, dstPort, dstAS.filter(_ == 0).map(" (" + _ + ")").getOrElse(""),
    proto, tos, pkts, bytes, stringExtra)
}

/**
 * Wrapper class for NetFlow Bytes
 * @param sender Sender Socket Address
 * @param msg Netty ByteBuf
 */
case class NetFlow(sender: InetSocketAddress, msg: ByteBuf)

/**
 * Wrapper class for SFlow Bytes
 * @param sender Sender Socket Address
 * @param msg Netty ByteBuf
 */
case class SFlow(sender: InetSocketAddress, msg: ByteBuf)
