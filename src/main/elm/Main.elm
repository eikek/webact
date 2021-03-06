module Main exposing (..)

import Api
import App.Data exposing (..)
import App.Update exposing (..)
import App.View exposing (..)
import Browser exposing (Document)
import Browser.Navigation exposing (Key)
import Data.Flags exposing (Flags)
import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (..)
import Ports
import Random
import Url exposing (Url)



-- MAIN


main =
    Browser.application
        { init = init
        , view = viewDoc
        , update = update
        , subscriptions = subscriptions
        , onUrlRequest = NavRequest
        , onUrlChange = NavChange
        }



-- MODEL


init : Flags -> Url -> Key -> ( Model, Cmd Msg )
init flags url key =
    let
        im =
            App.Data.init key url flags

        cmd =
            App.Update.initCmd im
    in
    ( im, Cmd.batch [ cmd, Ports.initElements (), Api.versionInfo VersionResp ] )


viewDoc : Model -> Document Msg
viewDoc model =
    { title = model.flags.appName
    , body = [ view model ]
    }



-- SUBSCRIPTIONS


subscriptions : Model -> Sub Msg
subscriptions model =
    Sub.none
