/*
 * Copyright (C) 2009-2010 Boris Okunskiy (http://incarnate.ru)
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY AUTHOR AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL AUTHOR OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */

package ru.circumflex.hibernate

import java.io.Serializable
import java.sql.Connection
import org.hibernate._

class HibernateSession(val session: Session) {
  // mail lifecycle methods
  def begin = session.getTransaction.begin
  def commit = session.getTransaction.commit
  def rollback = session.getTransaction.rollback
  def close = session.close
  // criteria creation methods
  def createCriteria[T](persistentClass: Class[T]): HibernateCriteria[T] =
    new HibernateCriteria[T](session.createCriteria(persistentClass))
  def createCriteria[T](persistentClass: Class[T], alias: String): HibernateCriteria[T] =
    new HibernateCriteria[T](session.createCriteria(persistentClass, alias))
  def createCriteria[T](entityName: String): HibernateCriteria[T] =
    new HibernateCriteria[T](session.createCriteria(entityName))
  def createCriteria[T](entityName: String, alias: String): HibernateCriteria[T] =
    new HibernateCriteria[T](session.createCriteria(entityName, alias))
  // query creation methods
  def createFilter(collection: Any, queryString: String) = session.createFilter(collection, queryString)
  def createQuery(queryString: String) = session.createQuery(queryString)
  def createSQLQuery(queryString: String) = session.createSQLQuery(queryString)
  def getNamedQuery(queryName: String) = session.getNamedQuery(queryName)
  // entity manipulation methods
  def get[E, I <: Serializable](persistentClass: Class[E], id: I): Option[E] = {
    val obj = session.get(persistentClass, id)
    if (obj == null) None
    else Some(obj.asInstanceOf[E])
  }
  def get[E, I <: Serializable](persistentClass: Class[E], id: I, lockMode: LockMode): Option[E] ={
    val obj = session.get(persistentClass, id, lockMode)
    if (obj == null) None
    else Some(obj.asInstanceOf[E])
  }
  def get[E, I <: Serializable](entityName: String, id: I): Option[E] = {
    val obj = session.get(entityName, id)
    if (obj == null) None
    else Some(obj.asInstanceOf[E])
  }
  def get[E, I <: Serializable](entityName: String, id: I, lockMode: LockMode): Option[E] = {
    val obj = session.get(entityName, id, lockMode)
    if (obj == null) None
    else Some(obj.asInstanceOf[E])
  }
  def load[E, I <: Serializable](persistentClass: Class[E], id: I): E =
    session.load(persistentClass, id).asInstanceOf[E]
  def load[E, I <: Serializable](persistentClass: Class[E], id: I, lockMode: LockMode): E =
    session.load(persistentClass, id, lockMode).asInstanceOf[E]
  def load[E, I <: Serializable](entityName: String, id: I): E =
    session.load(entityName, id).asInstanceOf[E]
  def load[E, I <: Serializable](entityName: String, id: I, lockMode: LockMode): E =
    session.load(entityName, id, lockMode).asInstanceOf[E]
  def delete(obj: Any) = session.delete(obj)
  def delete(entityName: String, obj: Any) = session.delete(entityName, obj)
  def lock(obj: Any, lockMode: LockMode) = session.lock(obj, lockMode)
  def lock(entityName: String, obj: Any, lockMode: LockMode) = session.lock(entityName, obj, lockMode)
  def merge[T](obj: T):T = session.merge(obj).asInstanceOf[T]
  def merge[T](entityName: String, obj: T):T = session.merge(entityName, obj).asInstanceOf[T]
  def persist(obj: Any) = session.persist(obj)
  def persist(entityName: String, obj: Any) = session.persist(entityName, obj)
  def refresh(obj: Any) = session.refresh(obj)
  def refresh(obj: Any, lockMode: LockMode) = session.refresh(obj, lockMode)
  def replicate(obj: Any, mode: ReplicationMode) = session.replicate(obj, mode)
  def replicate(entityName: String, obj: Any, mode: ReplicationMode) = session.replicate(entityName, obj, mode)
  def save(obj: Any) = session.save(obj)
  def save(entityName: String, obj: Any) = session.save(entityName, obj)
  def saveOrUpdate(obj: Any) = session.saveOrUpdate(obj)
  def saveOrUpdate(entityName: String, obj: Any) = session.saveOrUpdate(entityName, obj)
  def update(obj: Any) = session.update(obj)
  def update(entityName: String, obj: Any) = session.update(entityName, obj)
  // modes
  def getCacheMode = session.getCacheMode
  def setCacheMode(cacheMode: CacheMode) = session.setCacheMode(cacheMode)
  def getFlushMode = session.getFlushMode
  def setFlushMode(flushMode: FlushMode) = session.setFlushMode(flushMode)
  def setReadOnly(obj: Any, readOnly: Boolean) = session.setReadOnly(obj, readOnly)
  // others
  def connection = session.connection
  def disconnect = session.disconnect
  def reconnect(connection: Connection) = session.reconnect(connection)
  def isConnected = session.isConnected
  def isOpen = session.isOpen
  def isDirty = session.isDirty
  def cancelQuery = session.cancelQuery
  def clear = session.clear
  def contains(o: Any) = session.contains(o)
  def evict(o: Any) = session.evict(o)
  def flush = session.flush
  def transaction = session.getTransaction
  def statistics = session.getStatistics
  def sessionFactory = session.getSessionFactory

}

