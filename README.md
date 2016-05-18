# remlok

[![Clojars Project](https://img.shields.io/clojars/v/gumvic/remlok.svg)](https://clojars.org/gumvic/remlok)

This is a very small and simple web framework, which still aims to tackle complicated problems.

Think re-frame talking to the server out of the box.

## Prerequisites

If haven't already, you really want to read [re-frame tutorial](https://github.com/Day8/re-frame).

If you have used re-frame, you will get a grasp of this one in no time.

Also, note that remlok is a no-magic framework. 
It keeps things simple and predictable, but this also means that you shouldn't be afraid to get your hands dirty, since it doesn't do much by default.

## Usage

This is what happens in remlok:

1) Set up.

1.1) You **pub** and **mut** functions to handle reads and mutations.

1.2) If you want to talk to your remote, you **send** a function which will do that.
 
1.3) If you want to control how the response from the remote gets merged in the state, you **merge** a function which will do that.

2) Your reagent components **read** queries.

2.1) remlok uses the corresponding handler, or falls back to the default.

2.2) The read handler returns **{:loc, :rem}**; **:loc** is given to the component, **:rem** goes to the remote. 

3) Your user does something, and your components **mut!** that.

3.1) remlok uses the corresponding handler, or falls back to the default.

3.2) The mutation handler returns **{:loc, :rem}**; **:loc** is the new state, **:rem** goes to the remote.

4) The response from the remote comes back to be merged.

4.1) remlok uses the corresponding handler, or falls back to the default.

As you can see, remlok allows you to have your say on every step of the application lifecycle.
It also tries to be as predictable and reasonable with its default actions.

## Query

A query is just **[topic args]**, both for reads and mutations.

When you **pub/mut**, you set up the handler for the topic.

When you **merge**, you also set up the handler for the topic.

## Read

You set up your read functions with **pub**.

The read function must return 

```clojure
{:loc reaction 
 :rem query}
```

Both **:loc** and **:rem** are optional.

## Mutation

You set up your read functions with **mut**.

The mutation function must return 

```clojure
{:loc db* 
 :rem query}
```

Both **:loc** and **:rem** are optional.

## Send

You set up your send function with **send**.

This function will get **req** and **res** arguments.

**req** is the request.

**res** is a callback which your send function will have to call with the response, once it's available from the remote.

The request has the format

```clojure
{:reads [query0 query1 ...]
 :muts [query0 query1 ...]}
```

Both **:reads** and **:muts** are optional.

Note that remlok will be smart enough to batch the queries.

## Merge

You set up your merge function with **merge**, for the topic that you want to merge in some specific way.

Your merge handler will receive three arguments, **db**, **query** and **data**.

**db** - the current application state.

**query** - the query, the result of which you're merging.

**data** - the result itself.

When there is a novelty to be merged, it will be handled by the merge function.

The novelty will have the format 

```clojure
[[query0 data0] [query1 data1] ...]
```

As you can see, those are just **[query data]** pairs, where the **data** is the result of the corresponding **query**.

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
(merge!
  :user/new
  (fn [db [_ {tmp-id :id}] {id :id}]
    (let [user (get-in db [:users tmp-id])]
      (-> db
        (update :users dissoc tmp-id)
        (assoc-in [:users id] user)))))
```

Note that you can call **merge!** by yourself at any time with any properly formatted novelty.
This will be usable if you want to handle push updates from the remote (i. e. when there's no send before the merge).

## Remote

Remote is much more simple.

**remlok.rem** namespace exposes **pub**, **mut**, **read** and **mut!** functions.

**read** and **mut!** allow you to pass the **ctx**, any clojure value, which will be passed to your handler functions.

remlok has no further opinions on how you handle things on your server.

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

(Also, you can emulate recursive queries **to some extent**, having all that "friend of friend of friend" madness in your **args**.)

### Why request has **:reads** and **:muts**, but response (and novelty in general) do not?

The request sent to your remote has **:reads** and **:muts** to let your remote know how to process each query.

Since queries have exactly the same format for reads and mutations, this will let you know when to use **read** and **mut!** on the remote.

On the other hand, the response is just a vector of pairs **[query data]**.

That's because, from the client's point of view, all that comes from the remote is reads.
For example, if the client sends a mutation **[:user/new "Alice"]**, the response **[[:user/new "Alice"] {:id 1}]** is not a "mutation", it's a read of the result of that mutation.
Basically, the client sends reads and mutations, and says, "I want the response to be the **reads** of the results of all those operations I sent".

## License

Distributed under the Eclipse Public License, the same as Clojure.