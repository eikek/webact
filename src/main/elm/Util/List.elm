module Util.List exposing (distinct)


distinct: List a -> List a
distinct list =
    distinct1 list []

distinct1: List a -> List a -> List a
distinct1 list seen =
    case list of
        [] -> seen
        a :: rest ->
            if List.member a seen then (distinct1 rest seen)
            else distinct1 rest (a :: seen)
