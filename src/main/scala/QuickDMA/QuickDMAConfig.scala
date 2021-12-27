package QuickDMA

import chisel3._
import chisel3.util.log2Up
import freechips.rocketchip.config._


case object PECOL extends Field[Int]
case object PEROW extends Field[Int]
case object DISWIDTH extends  Field[Int]
case object BITWIDTH extends  Field[Int]
case object ADDRESSWIDTH extends  Field[Int]
case object FRONTEND_TILE_NUM extends  Field[Int]
case object MAX_BUCKET_NUM extends  Field[Int]
case object DELAY_TABLE_NUM extends  Field[Int]

class QuickDMAConfig extends Config((site, here, up) =>{
  case BITWIDTH => 32
  case PECOL => 16
  case PEROW => 4
  case DISWIDTH =>  32
  case ADDRESSWIDTH => 16
  case FRONTEND_TILE_NUM => 8
  case MAX_BUCKET_NUM => 1024
  case DELAY_TABLE_NUM => 4
})

trait QuickFPSParams{
  def bitwidth(implicit p:Parameters): Int = p(BITWIDTH)
  def pecol(implicit p:Parameters): Int = p(PECOL)
  def perow(implicit p:Parameters): Int = p(PEROW)
  def dist_width(implicit p:Parameters): Int = p(DISWIDTH)
  def addr_width(implicit p:Parameters): Int = p(ADDRESSWIDTH)
  def max_bucket_num(implicit p:Parameters): Int = p(MAX_BUCKET_NUM)

  def delay_table_num(implicit p:Parameters): Int = p(DELAY_TABLE_NUM)

  def frontend_tile_num(implicit p:Parameters): Int = p(FRONTEND_TILE_NUM)
  def boundbox_lines_num(implicit p:Parameters): Int = (p(MAX_BUCKET_NUM) / p(FRONTEND_TILE_NUM)).intValue()
  def dim: Int = 3
  def control_addr_width: Int = 5
}

class Point(implicit p: Parameters) extends Bundle with QuickFPSParams{
  val value  = Vec(dim,SInt(bitwidth.W))
  def xyz = Seq(value.apply(0), value.apply(1),value.apply(2))
  def zero = Seq(0.S(bitwidth.W), 0.S(bitwidth.W),0.S(bitwidth.W))
}

class PointWithDistance(implicit p: Parameters) extends Point with QuickFPSParams{
  val distance = SInt(bitwidth.W)
}

class WRIO[T <: Data](gen:T, val addr_width: Int) extends Bundle{
  val raddr = Input(UInt(addr_width.W))
  val rdata = Output(gen)
  val ren = Input(Bool())

  val waddr = Input(UInt(addr_width.W))
  val wdata = Input(gen)
  val wen = Input(Bool())
}
class FrontEnd_DataIO(implicit p:Parameters) extends Bundle
  with QuickFPSParams
{
  val point = new Point()
  val num = UInt(log2Up(delay_table_num).W)
  val idx = UInt(log2Up(delay_table_num).W)
  val bucket_idx = UInt(log2Up(max_bucket_num).W)
}

class FrontEnd_HeadIO(implicit p:Parameters) extends Bundle
  with QuickFPSParams
{
  val num = UInt(log2Up(delay_table_num).W)
  val frontend_id = UInt(log2Up(frontend_tile_num).W)
}






