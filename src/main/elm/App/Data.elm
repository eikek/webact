module App.Data exposing (..)

import Http
import Data.Flags exposing (Flags)
import Data.Script exposing (Script)
import Page.Listing.Data

type alias Model =
    { flags: Flags
    , listingModel: Page.Listing.Data.Model
    }

init: Flags -> Model
init flags =
    { flags = flags
    , listingModel = Page.Listing.Data.emptyModel
    }

type Msg
    = AllScripts (Result Http.Error (List Script))
    | ListingMsg Page.Listing.Data.Msg
