module Main exposing (..)

import Browser
import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (..)
import Random

import Api
import Data.Flags exposing (Flags)
import App.Data exposing (..)
import App.Update exposing (..)
import App.View exposing (..)


-- MAIN


main =
  Browser.element
    { init = init
    , update = update
    , subscriptions = subscriptions
    , view = view
    }


-- MODEL


init : Flags -> (Model, Cmd Msg)
init flags =
  ( App.Data.init flags
  , Api.fetchScripts flags.apiBase AllScripts
  )


-- SUBSCRIPTIONS


subscriptions : Model -> Sub Msg
subscriptions model =
  Sub.none
