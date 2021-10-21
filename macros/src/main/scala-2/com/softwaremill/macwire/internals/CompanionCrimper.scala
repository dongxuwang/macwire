package com.softwaremill.macwire.internals

import scala.reflect.macros.blackbox

private[macwire] class CompanionCrimper[C <: blackbox.Context, T: C#WeakTypeTag](val c: C, log: Logger) {
  import c.universe._

  type DependencyResolverType = DependencyResolver[c.type, Type, Tree]

  lazy val targetType: Type = implicitly[c.WeakTypeTag[T]].tpe

  lazy val companionType: Option[Type] = CompanionCrimper.companionType(c)(targetType)

  lazy val applies: Option[List[Symbol]] = CompanionCrimper.applies(c, log)(targetType)

  def applyTree(dependencyResolver: DependencyResolverType): Option[Tree] =
    CompanionCrimper.applyTree[C](c, log)(targetType, dependencyResolver.resolve(_, _))

}

object CompanionCrimper {
  private def showApply[C <: blackbox.Context](c: C)(s: c.Symbol): String = s.asMethod.typeSignature.toString

  private def isCompanionApply[C <: blackbox.Context](c: C)(targetType: c.Type, method: c.Symbol): Boolean =
    method.isMethod &&
      method.isPublic &&
      method.asMethod.returnType <:< targetType &&
      method.asMethod.name.decodedName.toString == "apply"

  private def companionType[C <: blackbox.Context](c: C)(targetType: c.Type): Option[c.Type] = {
    import c.universe._

    if (targetType.companion == NoType) None else Some(targetType.companion)
  }

  private def applies[C <: blackbox.Context](c: C, log: Logger)(targetType: c.Type): Option[List[c.Symbol]] =
    log.withBlock("Looking for apply methods of Companion Object") {
      val as: Option[List[c.Symbol]] =
        companionType(c)(targetType).map(_.members.filter(CompanionCrimper.isCompanionApply(c)(targetType, _)).toList)
      as.foreach(x => log.withBlock(s"There are ${x.size} apply methods:") { x.foreach(s => log(showApply(c)(s))) })
      as
    }

  def applyTree[C <: blackbox.Context](
      c: C,
      log: Logger
  )(targetType: c.Type, resolver: (c.Symbol, c.Type) => c.Tree): Option[c.Tree] = {
    import c.universe._

    lazy val apply: Option[Symbol] = CompanionCrimper
      .applies(c, log)(targetType)
      .flatMap(_ match {
        case applyMethod :: Nil => Some(applyMethod)
        case _                  => None
      })

    lazy val applySelect: Option[Select] = apply.map(a => Select(Ident(targetType.typeSymbol.companion), a))

    lazy val applyParamLists: Option[List[List[Symbol]]] = apply.map(_.asMethod.paramLists)

    def wireParams(paramLists: List[List[Symbol]]): List[List[Tree]] =
      paramLists.map(_.map(p => resolver(p, p.typeSignature)))

    def applyArgs: Option[List[List[Tree]]] = applyParamLists.map(x => wireParams(x))

    for {
      pl: List[List[Tree]] <- applyArgs
      applyMethod: Tree <- applySelect
    } yield pl.foldLeft(applyMethod)((acc: Tree, args: List[Tree]) => Apply(acc, args))
  }

}
