## TODO

### Enhancements

- Consider adding middleware. Registered for `:remlok/mware` topic, it will be `handler -> handler*`

- Consider multiple remotes. {:loc, :http-a, :http-b, :ws, ...}.
`send` should be topic-parametrized, too.
Update README, kill the `locrem` thing.

- remote `pubf` and `mutf` should emit cross-platform warning that they don't do anything.

### Misc
 
- Add production `lein` options.

- There should be at least some tests.