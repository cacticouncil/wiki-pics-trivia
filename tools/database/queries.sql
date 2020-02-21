-- get a single random question (used and standard category can be specified)
select a.id as aid, a.topic as tid, a.text as answer, t.mid, t.wid, p.id as pid, p.question, c.id as cid
from answers a
join properties p            on p.id=a.property
join topics t                on t.id=a.topic
join topics_to_categories tc on tc.topic=a.topic
join categories c            on c.id=tc.category
where a.used in (0,0) and c.standard in (0,1)
order by RANDOM()
limit 1;

-- get a single random question from a category of a specific mode
select a.id as aid, a.topic as tid, a.text as answer, t.mid, t.wid, p.id as pid, p.question, c.id as cid
from answers a
join properties p            on p.id=a.property
join topics t                on t.id=a.topic
join topics_to_categories tc on tc.topic=a.topic
join categories c            on c.id=tc.category
join modes_to_categories mc  on mc.category=c.id
where a.used in (0,0) and mc.mode=8
order by RANDOM()
limit 1;

-- get the name of a specific topic
select a.text as topic
from answers a
join properties p on p.id=a.property
where a.topic='443' and p.name='name';

-- get additional answers similar to an existing one (with same category and property)
select distinct a.text as answer
from answers a
join topics_to_categories tc on tc.topic=a.topic
join categories c on c.id=tc.category
where a.text<>'Boxing' and a.property=53 and c.id=56
order by random()
limit 3;

-- topic question answer report
select t.mid, a.text, p.question
from answers a, properties p, topics t
where a.property=p.id and a.topic=t.id
order by t.mid;