# remlok

[![Clojars Project](https://img.shields.io/clojars/v/gumvic/remlok.svg)](https://clojars.org/gumvic/remlok)

This is a very small and simple web framework, which still aims to tackle complicated problems.

Think re-frame talking to the server out of the box.

## Prerequisites

If haven't already, you really want to read [re-frame tutorial](https://github.com/Day8/re-frame).

If you have used re-frame, you will get a grasp of this one in no time.

Understanding of how reagent components and reactions work is required.

Also, note that remlok is a no-magic framework. 
It keeps things simple and not surprising, but this also means that you shouldn't be afraid to get your hands dirty, since it doesn't do much by default.

## In a nutshell

This is what happens when you use remlok:

<pre>
db -&gt; <a href="#read">read</a> -&gt; :loc -&gt; <a href="#render">render</a> -&gt; user action -&gt; <a href="#mutation">mut!</a> -&gt; :loc -&gt; db*
        |                                       |
        v                                       v
      :rem                                    :rem
        |                                       |
         ------------&gt; <a href="#send">send</a> &lt;-------------------
                         |
                         v
                       <a href="#remote">remote</a>
                         |
                         v
                       <a href="#merge">merge!</a>
                         |
                         v
                        db*
</pre>

It's your typical [eternal cycle of data, flowing](https://github.com/Day8/re-frame#dispatching-events), but with a twist - it has a branch which leads to the remote.

As you can see, remlok allows you to have your say on every step of the application lifecycle.
It also tries to be as predictable and reasonable as possible with its default actions.

## Locrem

Read and mutation functions return locrems.

A locrem is a map

```clojure
{:loc local-result 
 :rem send-this-to-remote}
```

Both `:loc` and `:rem` are optional.

## Query

A query is a pair

```clojure
[topic args]
```

Both for reads and mutations.

It is considered appropriate to omit the `args`, e. g. `[:cur-user]`, `[:log-out]` etc.

## Read

You set up your read functions with `pub` like this

```clojure
(pub
  :cur-user
  (fn [db _] ;; this is the read function
    {:loc (reaction 
            (get @db :cur-user))}))
```

remlok will use the query's topic to decide on the read function. 

Read function will receive two arguments, `db` and `query`.

`db` - the application state ratom.

`query` - the query to read.

Read function must return this [locrem](#locrem) 

```clojure
{:loc reaction 
 :rem query}
```

## Render

Just use reagent components.

```clojure
(defn users []
  (let [users (read [:users {:first 10}])]
    (fn []
      [:ul
        (for [{:keys [id name]} @users]
          ^{:key id}
          [:li name])])))
```

## Mutation

You set up your mutation functions with `mut` like this

```clojure
(mut
  :logout
  (fn [db _] ;; this is the mutation function
    {:loc (dissoc db :cur-user)
     :rem [:log-out]}))
```

remlok will use the query's topic to decide on the mutation function. 

Mutation function will receive two arguments, `db` and `query`.

`db` - the application state.

`query` - the query to read.

Mutation function must return this [locrem](#locrem) 

```clojure
{:loc db* 
 :rem query}
```

## Send

You set up your send function with `send` like this

```clojure
(send
  (fn [req res] ;; this is the send function
    (my-network/send
      (my-edn/serialize req)
      (comp res my-edn/deserialize))))
```

Send function will receive `req` and `res` arguments.

`req` - the request.

`res` - the callback to call with the [response](#novelty), once it's available from the remote.

The request has the format

```clojure
{:reads [query0 query1 ...]
 :muts [query0 query1 ...]}
```

Note that remlok will be smart enough to batch the queries.

## Novelty

The novelty must have the format

```clojure
[[query0 data0] [query1 data1] ...]
```

## Merge

You set up your merge function with `merge` like this

```clojure
(merge
  :new-score
  (fn [db _ score] ;; this is the merge function
    (update db :score + score)))
```

remlok will use the query's topic to decide on the merge function.

Merge function will receive three arguments, `db`, `query` and `data`.

`db` - the application state.

`query` - the query, the result of which you're merging.

`data` - the result itself.

Merge function must return the new application state.

How does merging work?

The function which merges a novelty is called `merge!`.
remlok will call it for you, when your send function calls its `res` callback.

As we already know, the novelty should have the format 

```clojure
[[query0 data0] [query1 data1] ...]
```

As you can see, those are just `[query data]` pairs, where the `data` is the result of the corresponding `query`.

For example, if you have a request

```clojure
{:reads [[:user 1] [:user 2]]
 :muts [[:user/new {:id "tmp_id_1" :name "Alice"}]]}
```

you may receive

```clojure
[[[:user 1] {:id 1 :name "Bob"}]
 [[:user 2] {:id 2 :name "Shmob"}]
 [[:user/new {:id "tmp_id_1" :name "Alice"}] {:id 3}]]
```

By setting up merge handlers for the topics, you can control how all those things are getting integrated into your application state.

For example, you may want to patch your temporary ids like this (super naive but demonstrates the point):

```clojure
(merge
  :user/new
  (fn [db [_ {tmp-id :id}] {id :id}]
    (let [user (get-in db [:users tmp-id])]
      (-> db
        (update :users dissoc tmp-id)
        (assoc-in [:users id] user)))))
```

Note that you can call `merge!` by yourself at any time with any properly formatted novelty.
This will be usable if you want to handle push updates from the remote (i. e. when there's no send before the merge).

## Remote

`remlok.rem` namespace exposes `pub`, `mut`, `read` and `mut!` functions, along with the fallbacks `pubf` and `mutf`.

`read` and `mut!` allow you to pass the `ctx`, any clojure value, which will be passed to your handler functions.

remlok has no further opinions on how you handle things on your server.

Something like this:

```clojure
(pub
  :users
  (fn [db-conn [_ {:keys [name]}]]
    (my-sql-lib/select
      db-conn
      "select * from users where name = :name"
      {:name name})))

(def db-conn 
  (my-sql-lib/open-connection))

(defn endpoint [req]
  (let [{:keys [reads]} (my-edn/deserialize req)
        res (for [query reads]
              [query (read db-conn query)])]
    (my-edn/serialize res)))
    
(my-network/listen!
  80
  endpoint)
```

## Fallbacks

remlok provides fallbacks for everything, so it can function on its own, without you specifying a single handler.
(Obviously, the send fallback doesn't actually do anything except emitting a warning that it doesn't do anything.)

Fallbacks have **f** at the end - `pubf`, `mutf`, `sendf` and `mergef`, and are public.
Their docstrings explain what they do (they don't do a whole lot).

Read, mutation and merge fallbacks are initially registered for `:remlok/default` topic.
When remlok can't find the handler for a topic, it just uses whatever handles `:remlok/default`.
Note that the handler is given the initial query (and, therefore, the initial topic).

Of course, you can set up your own fallback (the "default" handler):

```clojure
(pub
  :remlok/default
  (fn [db query]
    (println "Warning! Unknown query " (str query))
    nil ;; nil is explicit to emphasize that we are returning an empty locrem
    ))
```

Note that even if you set up your own default handler, you still can use default fallbacks:

```clojure
(pub
  :search-input
  pubf ;; simple enough for pubf to handle
  )
```

## Examples

Check out the examples - [remlok-examples](https://github.com/gumvic/remlok-examples).

They feature optimistic updates and all that!

## FAQ

### Why remlok?

Because you will learn it in dozens of minutes, and it will let you do things that are still often deemed non-trivial. 

(Of course, I'm supposing that you already can read and write Clojure and know what reagent is all about.)

### Why not declarative queries like in om.next?

Well, first of all, your queries is just data, so they **are** declarative; they are just not nested out of the box.

It was a very deliberate decision to keep the queries flat, since the API and all the machinery was getting seriously complicated, and remlok was on the verge of stopping being "miniature".

So, much like in re-frame, you can not nest queries, but I strongly believe that not all applications actually need recursive/deeply nested queries.

(Actually, feel free to check **recursive-queries** branch, which is trying to have om.next-like queries.)

(Also, you can emulate recursive queries **to some extent**, having all that "friend of friend of friend" madness in your `args`.)

### Why request has :reads and :muts, but response (and novelty in general) does not?

The request sent to your remote has `:reads` and `:muts` to let your remote know how to process each query.

Since queries have exactly the same format for reads and mutations, this will let you know when to use `read` and `mut!` on the remote.

On the other hand, the response is just a vector of pairs `[query data]`.

That's because, from the client's point of view, all that comes from the remote is reads.
For example, if the client sends a mutation `[:user/new "Alice"]`, the response `[[:user/new "Alice"] {:id 1}]` is not a "mutation", it's a read of the result of that mutation.
Basically, the client sends reads and mutations, and says, "I want the response to be the **reads** of the results of all those operations I sent".

### Why global state?

Just like re-frame, remlok uses global state, so you can have only one application per client context (and only one application per server context, for that matter).

Of course, this solution isn't quite optimal, so any feedback is welcome!

### Why should I handle requests by hand on the remote?

Since you may be composing your response in a non-trivial fashion. 

One of the examples, "Wiki Autocompleter", features such a case (it uses core.async to wait for the reads to be completed).

## License

Distributed under the Eclipse Public License, the same as Clojure.