module App.Update exposing (update)

import App.Data exposing (..)
import Page.Listing.Data
import Page.Listing.Update

update: Msg -> Model -> (Model, Cmd Msg)
update msg model =
    case msg of
        AllScripts res ->
            updateListing (Page.Listing.Data.AllScripts res) model

        ListingMsg lm ->
            updateListing lm model

updateListing: Page.Listing.Data.Msg -> Model -> (Model, Cmd Msg)
updateListing lmsg model =
    let
        (lm, lc) = Page.Listing.Update.update lmsg model.listingModel
    in
        ( {model | listingModel = lm }
        , Cmd.map ListingMsg lc
        )
