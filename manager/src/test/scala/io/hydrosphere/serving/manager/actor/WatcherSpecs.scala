package io.hydrosphere.serving.manager.actor

import akka.actor.ActorSystem
import io.hydrosphere.serving.manager.LocalModelSourceConfiguration
import io.hydrosphere.serving.manager.actor.modelsource.LocalSourceWatcher
import io.hydrosphere.serving.manager.service.modelsource.LocalModelSource
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import akka.testkit.{ImplicitSender, TestActors, TestKit}

class WatcherSpecs extends TestKit(ActorSystem("MySpec")) with ImplicitSender with Matchers with WordSpecLike with BeforeAndAfterAll {

  val localSource = new LocalModelSource(LocalModelSourceConfiguration("test", "/Users/bulat/Documents/Dev/Provectus/hydro-serving/manager/src/test/resources/test_models"))
  val localWatcher = system.actorOf(LocalSourceWatcher.props(localSource))

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "LocalSourceWatcher" must {
    "detect creation" in {

    }

    "detect a deletion of a model" in {

    }

    "detect a change of a model" in {

    }
  }
}
