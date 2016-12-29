package com.softwaremill.macwire.macwireakka

import com.softwaremill.macwire.CompileTestsSupport

class CompileTests extends CompileTestsSupport {

  runTestsWith(
    expectedFailures = List(
      "wireProps-3-missingDependency" -> List("Cannot find a value of type: [A]"),
      "wireAnonymousActor-3-missingDependency" -> List("Cannot find a value of type: [A]"),
      "wireActor-3-missingDependency" -> List("Cannot find a value of type: [A]"),
      "wireAnonymousActor-3.1-missingActorRefFactoryDependency" -> List("Cannot find a value of type: [akka.actor.ActorRefFactory]"),
      "wireActor-3.1-missingActorRefFactoryDependency" -> List("Cannot find a value of type: [akka.actor.ActorRefFactory]"),
      "wireProps-6-injectAnnotationButNoDependencyInScope" -> List("Cannot find a value of type: [C]"),
      "wireProps-7-notActor" -> List("type arguments [NotActor] do not conform to macro method wireProps's type parameter bounds [T <: akka.actor.Actor]")
    ),
    expectedWarnings = List()
  )
}
