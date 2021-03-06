package QuickDMA

import chisel3._
import chisel3.util.experimental.loadMemoryFromFile
import chisel3.util.{Cat, Counter, Decoupled, DecoupledIO, Enum, Queue, RRArbiter, Valid, ValidIO, is, log2Up, switch}
import freechips.rocketchip.config._


object DmaSize {
  private val enums = Enum(8)
  val Seq(bytes, wordHalf, word, wordDouble, wordQuad, word8, word16, word32) = enums
  def gen: UInt = chiselTypeOf(enums.head)
}

class DmaControl extends Bundle {
  val index = UInt(32.W)
  val length = UInt(32.W)
  val size = DmaSize.gen
}

class DmaIO(val dmaWidth: Int) extends Bundle {
  val Seq(readControl, writeControl) = Seq.fill(2)(Decoupled(new DmaControl))
  val readChannel = Flipped(Decoupled(UInt(dmaWidth.W)))
  val writeChannel = Decoupled(UInt(dmaWidth.W))
}

class DmaRequest(val memorySize: Int) extends Bundle {
  val index = UInt(log2Up(memorySize).W)
  val length = UInt(log2Up(memorySize).W)
  val tpe = Bool()
}

object DmaRequest {
  val read: Bool = false.B
  val write: Bool = true.B

  def init_(memorySize: Int) = {
    val a = Wire(new Valid(new DmaRequest(memorySize)))
    a.valid := false.B
    a.bits.index := DontCare
    a.bits.length := DontCare
    a.bits.tpe := DontCare
    a
  }
}

class Dma[A <: Data](size: Int, gen: A, initFile: Option[String] = None) extends Module {

  private val dmaWidth = gen.getWidth

  val io = IO(Flipped(new DmaIO(dmaWidth)))

  val req = RegInit(DmaRequest.init_(size))

  /* Only one outstanding read or write request at a time */
  Seq(io.readControl, io.writeControl).map(_.ready := !req.valid)

  val arb = Module(new RRArbiter(new DmaControl, 2))
  arb.io.in
    .zip(Seq(io.readControl, io.writeControl))
    .map{ case (a, b) => a <> b }

  arb.io.out.ready := !req.valid
  when (arb.io.out.fire) {
    req.valid := true.B
    req.bits.index := arb.io.out.bits.index
    req.bits.length := arb.io.out.bits.length
    req.bits.tpe := arb.io.chosen
  }

  /* Defaults */
  io.writeChannel.ready := false.B

  /** Queue of read responses */
  val readQueue: Queue[A] = Module(new Queue(gen, 8))
  readQueue.io.deq <> io.readChannel
  assert(!readQueue.io.enq.valid || readQueue.io.enq.ready, "Response Queue dropped input data!")

  /** Queue of write requests */
  val writeQueue: Queue[A] = Module(new Queue(gen, 8))
  writeQueue.io.enq.valid := io.writeChannel.valid
  writeQueue.io.enq.bits := io.writeChannel.bits
  io.writeChannel.ready := writeQueue.io.enq.ready && (req.bits.tpe === DmaRequest.write)

  /** Asserted if it is safe to send a ballistic request to the memory that will be caught by the [[readQueue]]. This
   * implies that the [[Dma]] unit is processing a read request and the [[readQueue]] will not be full by the time the
   * data gets there.
   */
  val doRead: Bool = req.valid &&
    (req.bits.tpe === DmaRequest.read) &&
    (readQueue.io.count < (readQueue.entries - 2).U) &&
    (req.bits.length =/= 0.U)

  /** Asserted if it is safe to send a write to the memory. */
  val doWrite: Bool = req.valid &&
    (req.bits.tpe === DmaRequest.write) &&
    writeQueue.io.deq.valid &&
    (req.bits.length =/= 0.U)

  /* Synchronous Read Memory that encapsulates the virtual memory space of the accelerator */
  val mem: SyncReadMem[A] = SyncReadMem(size, gen.cloneType)
  //initFile.map(loadMemoryFromFile(mem, _))

  readQueue.io.enq.bits := mem.read(req.bits.index)

  /* Allow a read to go to the Arbiter */
  readQueue.io.enq.valid := RegNext(doRead)
  when (doRead) {
    req.bits.index := req.bits.index + 1.U
    req.bits.length := req.bits.length - 1.U
  }

  /* When the request is done, then reset the request register */
  when (req.valid && (req.bits.length === 0.U) && !readQueue.io.deq.valid && !writeQueue.io.deq.valid) {
    req.valid := false.B
  }

  /* Allow a write to go to the memory */
  writeQueue.io.deq.ready := doWrite
  when (doWrite) {
    req.bits.index := req.bits.index + 1.U
    req.bits.length := req.bits.length - 1.U
    mem.write(req.bits.index, writeQueue.io.deq.bits)
  }

}