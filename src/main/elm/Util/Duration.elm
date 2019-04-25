module Util.Duration exposing (..)

type alias Duration = Int


toHuman: Duration -> String
toHuman dur =
    (String.fromInt dur) ++ "ms"
