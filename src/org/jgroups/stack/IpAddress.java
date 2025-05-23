package org.jgroups.stack;

import org.jgroups.Address;
import org.jgroups.Constructable;
import org.jgroups.Global;
import org.jgroups.PhysicalAddress;

import java.io.*;
import java.net.*;
import java.util.function.Supplier;

/**
 * Network-dependent address (Internet). Generated by the bottommost layer of the protocol
 * stack (UDP). Contains an InetAddress and port.
 * @author Bela Ban
 */
public class IpAddress implements PhysicalAddress, Constructable<IpAddress> {
    protected InetAddress ip_addr;
    protected int         port;


    // Used only by marshalling
    public IpAddress() {
    }

    /** e.g. 192.168.1.5:7800 */
    public IpAddress(String addr_port) throws Exception {
        int index=addr_port.lastIndexOf(':');
        if(index == -1)
            setAddress(getByName(addr_port), 0);
        else
            setAddress(getByName(addr_port.substring(0, index)), Integer.parseInt(addr_port.substring(index+1)));
    }

    public IpAddress(String i, int p) throws UnknownHostException {
        setAddress(getByName(i), p);
    }

    public IpAddress(InetAddress i, int p) {
        setAddress(i, p);
    }

    public IpAddress(int p) {
        setAddress(getLocalHost(), p);
    }

    public IpAddress(InetSocketAddress sock_addr) {
        setAddress(sock_addr.getAddress(), sock_addr.getPort());
    }

    public Supplier<? extends IpAddress> create() {
        return IpAddress::new;
    }

    public static InetAddress getByName(String host) throws UnknownHostException {
        if(host == null || host.isEmpty())
            return getLocalHost();
        return InetAddress.getByName(host);
    }

    public static InetAddress getLocalHost() {
        try {
            return InetAddress.getLocalHost();  // get first NIC found (on multi-homed systems)
        }
        catch(Exception e) {
        }
        try {
            return InetAddress.getByName(null);
        }
        catch(UnknownHostException e) {
        }
        return null;
    }

    @Override public InetAddress getIpAddress() {return ip_addr;}
    @Override public int         getPort()      {return port;}

    /**
     * implements the java.lang.Comparable interface
     * @see java.lang.Comparable
     * @param o - the Object to be compared
     * @return a negative integer, zero, or a positive integer as this object is less than,
     *         equal to, or greater than the specified object.
     * @exception java.lang.ClassCastException - if the specified object's type prevents it
     *            from being compared to this Object.
     */
    public int compareTo(Address o) {
        int h1, h2, rc; // added Nov 7 2005, makes sense with canonical addresses

        if(this == o) return 0;
        if(!(o instanceof IpAddress))
            throw new ClassCastException("comparison between different classes: the other object is " +
                    (o != null? o.getClass() : o));
        IpAddress other = (IpAddress) o;
        if(ip_addr == null)
            if (other.ip_addr == null) return Integer.compare(port, other.port);
            else return -1;

        h1=ip_addr.hashCode();
        h2=other.ip_addr.hashCode();
        rc=Integer.compare(h1, h2);
        return rc != 0 ? rc : Integer.compare(port, other.port);
    }

    public boolean equals(Object obj) {
        if(this == obj) return true; // added Nov 7 2005, makes sense with canonical addresses

        if(!(obj instanceof IpAddress))
            return false;
        IpAddress other=(IpAddress)obj;
        boolean sameIP;
        if(this.ip_addr != null)
            sameIP=this.ip_addr.equals(other.ip_addr);
        else
            sameIP=(other.ip_addr == null);
        return sameIP && (this.port == other.port);
    }

    public int hashCode() {
        return ip_addr != null ? ip_addr.hashCode() + port : port;
    }

    public String toString() {
        return printIpAddress();
    }

    public String printIpAddress() {
        return String.format("%s:%d", ip_addr != null? ip_addr.getHostAddress() : "<null>", port);
    }

    public String printIpAddress2() {
        return String.format("%s[%d]", ip_addr != null? ip_addr.getHostAddress() : "localhost", port);
    }

    public String printHostAddress() {
        return ip_addr != null? ip_addr.getHostAddress() : "";
    }

    @Override
    public void writeTo(DataOutput out) throws IOException {
        if(ip_addr != null) {
            byte[] address=ip_addr.getAddress();  // 4 bytes (IPv4) or 16 bytes (IPv6)
            out.writeByte(address.length); // 1 byte
            out.write(address, 0, address.length);
            if(ip_addr instanceof Inet6Address)
                out.writeInt(((Inet6Address)ip_addr).getScopeId());
        }
        else {
            out.writeByte(0);
        }
        out.writeShort(port);
    }

    @Override
    public void readFrom(DataInput in) throws IOException {
        InetAddress ip=null;
        int len=in.readByte();
        if(len > 0) {
            if(len != Global.IPV4_SIZE && len != Global.IPV6_SIZE)
                throw new IOException(String.format("length has to be %d or %d bytes (was %d bytes)",
                                                    Global.IPV4_SIZE, Global.IPV6_SIZE, len));
            byte[] a=new byte[len]; // 4 bytes (IPv4) or 16 bytes (IPv6)
            in.readFully(a);
            if(len == Global.IPV6_SIZE) {
                int scope_id=in.readInt();
                ip=Inet6Address.getByAddress(null, a, scope_id);
            }
            else {
                ip=InetAddress.getByAddress(a);
            }
        }
        // changed from readShort(): we need the full 65535, with a short we'd only get up to 32K !
        int p=in.readUnsignedShort();
        setAddress(ip, p);
    }

    @Override
    public int serializedSize() {
        // length (1 bytes) + 2 bytes for port
        int tmp_size=Global.BYTE_SIZE+ Global.SHORT_SIZE;
        if(ip_addr != null) {
            // 4 bytes for IPv4, 20 for IPv6 (16 + 4 for scope-id)
            tmp_size+=(ip_addr instanceof Inet4Address)? 4 : 20;
        }
        return tmp_size;
    }

    public IpAddress copy() {
        return new IpAddress(ip_addr, port);
    }

    protected void setAddress(InetAddress i, int p) {
        ip_addr=i; // != null? i : getLocalHost();
        port=p;
    }
}
