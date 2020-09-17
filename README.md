# injectable

Injectable is a(nother) lightweight IoC container for Clojure(script).
It is very similar to Integrant in scope. Similar to Integrant, it removes some
limitations from Component linked to the decision to favour inmutable records.
Injectable, like Integrant, doesn't imposo this limitation, giving a more natural
injection mechanism and even allowing for circular dependencies.

Unlike Integrant, the implementation does not depend on multimethods, but in
plain functions. This is a matter of style and it doesn't prevent the mechanism
for building the objects to be externalised from the configuration of the system.

This library takes inspiration in the Spring container. The configurable elements are named
'beans' in the codebase due to this fact."

## Usage

FIXME

The documentation is not yet written. You can find simple (simplistic) examples
of usage in the tests.

Here below you can find an example taken from an example WIP application where
Injectable is used as the container:

     {:main-component [:ui-page-template default-content]
      ::top-row       [ui-top-row
                       ::app-icon
                       ::app-name
                       ::topbar-center
                       ::topbar-right
                       ::on-logo-click]
      ::app-icon      [:= [:div "*app-icon*"]]
      ::app-name      [:= [:div "*app-name*"]]
      ::topbar-center [:= [:div.ui.text.container
                           [ui-top-row-entry nil [:i.ui.upload.icon] "Upload"]
                           [ui-top-row-entry nil [:i.ui.clock.icon] "Recent"]
                           [ui-top-row-entry
                            nil
                            [:i.ui.envelope.icon]
                            "Notifications"
                            [:span.ui.label {:style {:font-size :xx-small}} 2]]]]
      ::topbar-right [ui-login-top-row]
      :ui-page-template [ui-page-template ::top-row '?]}

## License

Copyright © 2020 Pedro Abelleira Seco

This program and the accompanying materials are made available under the
terms of the EU Public License 1.2 or later which is available at
https://joinup.ec.europa.eu/sites/default/files/custom-page/attachment/2020-03/EUPL-1.2%20EN.txt
