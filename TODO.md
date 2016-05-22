## TODO

### Enhancements

1) Nested mutations, i. e., mutations kicking off other mutations.

2) Consider adding middleware. Registered for `:remlok/mware` topic, it will be `handler -> handler*`

3) Minor enhancements (see TODOs in the source).

4) Consider adding `send/sendf` on the remote (update README, the Fallback section).

5) Consider multiple remotes. {:loc, :http-a, :http-b, :ws, ...}.
`send` should be topic-parametrized, too.
Update README, kill the `locrem` thing.

### Misc
 
1) Add production `lein` options.

2) Consider creating `lein` template.

3) There should be at least some tests.