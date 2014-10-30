package io.garuda.common.builder

/**
 * Created by obergner on 19.04.14.
 */
trait Builder[X] {

  def build(): X
}

trait NodeBuilder[N, X] extends Builder[X] {

  def buildNode(builderContext: BuilderContext): N
}

trait HasParentNodeBuilder[M, PB <: NodeBuilder[M, X], N, X] {
  self: NodeBuilder[N, X] =>

  protected[this] def parentNodeBuilder: PB

  def build(): X = parentNodeBuilder.build()

  def end(): PB = parentNodeBuilder
}

case class LeafNodeProduct(child: Int)

case class InnerNodeProduct(parent: Int, child: LeafNodeProduct)

case class RootNodeProduct(grandParent: Int, child: InnerNodeProduct)

case class RootProduct(root: Int, nodeProduct: RootNodeProduct)

class RootProductBuilder extends NodeBuilder[RootNodeProduct, RootProduct] {

  private[this] var rootVal: Int = 0

  val innerNodeProductBuilder: InnerNodeProductBuilder = new InnerNodeProductBuilder(this)

  override def buildNode(builderContext: BuilderContext): RootNodeProduct =
    RootNodeProduct(rootVal, innerNodeProductBuilder.buildNode(builderContext))

  override def build(): RootProduct = RootProduct(1, buildNode(null))

  def root(root: Int): this.type = {
    rootVal = root
    this
  }
}

class InnerNodeProductBuilder(protected[this] val parentNodeBuilder: RootProductBuilder)
  extends NodeBuilder[InnerNodeProduct, RootProduct]
  with HasParentNodeBuilder[RootNodeProduct, RootProductBuilder, InnerNodeProduct, RootProduct] {

  private[this] var innerNodeVal: Int = 0

  val leafNodeProductBuilder: LeafNodeProductBuilder = new LeafNodeProductBuilder(this)

  override def buildNode(builderContext: BuilderContext): InnerNodeProduct =
    InnerNodeProduct(innerNodeVal, leafNodeProductBuilder.buildNode(builderContext))

  def innerNode(inner: Int): this.type = {
    innerNodeVal = inner
    this
  }
}

class LeafNodeProductBuilder(protected[this] val parentNodeBuilder: InnerNodeProductBuilder)
  extends NodeBuilder[LeafNodeProduct, RootProduct]
  with HasParentNodeBuilder[InnerNodeProduct, InnerNodeProductBuilder, LeafNodeProduct, RootProduct] {

  private[this] var leafNodeVal: Int = 0

  override def buildNode(builderContext: BuilderContext): LeafNodeProduct = LeafNodeProduct(leafNodeVal)

  def leafNode(leaf: Int): this.type = {
    leafNodeVal = leaf
    this
  }
}

object Sample extends App {

  val rootProductBuilder = new RootProductBuilder

  val rootProduct = rootProductBuilder.root(10).innerNodeProductBuilder.innerNode(20).leafNodeProductBuilder.leafNode(30).end().build()

  println(rootProduct)
}
