package controllers

import javax.inject._

import play.api.mvc._
import de.htwg.se.tablut.Tablut
import de.htwg.se.tablut.bcontroller.IController

import play.api.libs.streams.ActorFlow
import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.actor._

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.actions.SecuredRequest
import org.webjars.play.WebJarsUtil
import play.api.i18n.I18nSupport
import utils.auth.DefaultEnv
import scala.concurrent.Future

@Singleton
class TablutController @Inject() (
    cc: ControllerComponents,
    silhouette: Silhouette[DefaultEnv]
)(
    implicit
    webJarsUtil: WebJarsUtil,
    assets: AssetsFinder,
    system: ActorSystem,
    mat: Materializer
) extends AbstractController(cc) with I18nSupport {

  val gc = Tablut.getInstance().getController()

  def game = silhouette.SecuredAction.async { implicit request: SecuredRequest[DefaultEnv, AnyContent] =>
    Future.successful(Ok(views.html.game()))
  }

  def command(command: String) = silhouette.SecuredAction.async { implicit request: SecuredRequest[DefaultEnv, AnyContent] =>
    gc.funktion(command)
    Future.successful(Ok(gc.getMatrixSize().toString()))
  }

  def help = silhouette.SecuredAction.async { implicit request: SecuredRequest[DefaultEnv, AnyContent] =>
    Future.successful(Ok(views.html.help()))
  }

  /*
  def controls = silhouette.SecuredAction.async { implicit request: SecuredRequest[DefaultEnv, AnyContent] =>
    Future.successful(Ok(views.html.controls("Control menu")))
  }
*/

  def socket = WebSocket.accept[String, String] { request =>
    ActorFlow.actorRef { out =>
      println("Connect received")
      TablutWebSocketActorFactory.create(out)
    }
  }

  object TablutWebSocketActorFactory {
    def create(out: ActorRef) = {
      Props(new TablutWebSocketActor(out))
    }
  }

  class TablutWebSocketActor(out: ActorRef) extends Actor {
    def receive = {
      case msg: String =>
        if (msg != "player") {
          if (msg != "update") {
            gc.funktion("" + msg);
          }
          out ! (gc.buildString())
          println("Sent Message: " + msg)
        } else {
          if (gc.getPlayerTurn) {
            out ! ("player1");
          } else {
            out ! ("player2");
          }
        }
    }

  }
}