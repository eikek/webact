module Page.Listing.Update exposing (update)

import Page.Listing.Data exposing (..)

update: Msg -> Model -> (Model, Cmd Msg)
update msg model =
    case msg of
        AllScripts (Ok scripts) ->
            ( {model | list = scripts }
            , Cmd.none
            )

        AllScripts (Err err) ->
            ( model, Cmd.none )
