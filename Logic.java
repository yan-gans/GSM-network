import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Logic implements Relation
{
    public static void main(String[] args) {
        new Logic();
    }
    private ArrayList<Layer> layers;
    public Logic()
    {
        this.layers=new ArrayList<>();
        VRDS vrds=new VRDS();
        BTSLayer btsLayer=new BTSLayer(vrds);
        BSCLayer bscLayer=new BSCLayer(btsLayer);
        BTSLayer btsLayer1=new BTSLayer(bscLayer);
        VBDS vbds= new VBDS(btsLayer1,vrds);
        this.layers.add(vbds);
        this.layers.add(btsLayer1);
        this.layers.add(bscLayer);
        this.layers.add(btsLayer);
        this.layers.add(vrds);
    }
    public void createBSCLayer()
    {
        this.layers.add(2,new BSCLayer(this.layers.get(2)));
        this.layers.get(1).next=this.layers.get(2);
    }

    @Override
    public int getLayers() {
        return this.layers.size()-4;
    }
    @Override
    public int[] getStations() {
        int[] a=new int[this.layers.size()];
        int i=0;
        for(Layer l:this.layers)
        {
            a[i]=l.stations.size();
            i++;
        }
        return a;
    }
    @Override
    public void removeBSCLayer() {
        this.layers.get(2).extremePass();
        this.layers.remove(2);
    }

    class VBDS extends Layer
    {
        private VRDS vrds;
        VBDS(Layer next, VRDS vrds)
        {
            super(next);
            this.vrds=vrds;
            this.stations=new ArrayList<>();
            this.stations.add(new VBD(this));
        }
    }
    class VBD extends Station
    {
        private volatile boolean suspended;
        private final SMS sms;
        VBD(VBDS current)
        {
            super(current);
            this.suspended=false;
            Scanner scanner=new Scanner(System.in);
            System.out.println("Enter message");
            this.sms=new SMS(this.number,(int)(Math.random()*((VBDS)(this.current)).vrds.stations.size()+1),scanner.next());
            this.start();
            current.stations.add(new VBD(current));
        }
        public void setDelay(long delay)
        {
            this.delay=delay;
        }
        public void endVBD()
        {
            this.running=false;
        }
        public void suspendVBD()
        {
            this.suspended=true;
        }
        public void resumeVBD()
        {
            this.suspended=false;
            synchronized (this)
            {
                notify();
            }
        }
        @Override
        public void run()
        {
            while (running)
            {
                while (suspended)
                {
                    try
                    {
                        wait();
                    }
                    catch (InterruptedException e)
                    {
                        throw new RuntimeException(e);
                    }
                }
                this.passTo(this.current.next.findMin(this.sms),this.sms);
                try
                {
                    Thread.sleep(this.delay);
                }
                catch (InterruptedException ignored){}
            }
        }
    }
    class SMS
    {
        private int sender;
        private int recipient;
        private String message;

        public void setRecipient(int recipient)
        {
            this.recipient = recipient;
        }
        SMS(int sender,int recipient, String message)
        {
            this.message=message;
            this.sender=sender;
            this.recipient=recipient;
        }
    }

    class BTSLayer extends Layer
    {
        public void createNew()
        {
            this.stations.add(new BTSL(this));
        }
        @Override
        public Station findMin(SMS sms) {
            Station s= super.findMin(sms);
            if(this.isPiled())this.createNew();
            return this.stations.get(this.stations.size()-1);
        }
        BTSLayer(Layer next)
        {
            super(next);
            this.stations=new ArrayList<>();
            this.stations.add(new BTSL(this));
        }
    }
    abstract class Layer
    {
         Layer next;
         ArrayList<Station> stations;
        Layer(Layer next)
        {
            this.next=next;
        }
        public void extremePass()
        {
            for(Station s :this.stations)
            {
                s.extremePass();
                s.running=false;
            }
        }
        public void removeStation(Station station)
        {
            this.stations.remove(station);
        }
        public Station findMin(SMS sms)
        {
            Station tmp=this.next.stations.get(0);
            for(int i=1;i<this.next.stations.size();i++)
            {
                if(tmp.record.size()>this.next.stations.get(i).record.size())
                    tmp=this.next.stations.get(i);
            }
            return tmp;
        }
        public boolean isPiled()
        {
            for(Station e: this.stations)
            {
                if(e.record.size()<5)
                    return false;
            }
            return true;
        }
    }
    class BTSL extends Station
    {
        BTSL(BTSLayer current)
        {
            super(current);
            this.start();
        }
    }
    class BSC extends Station
    {
        BSC(BSCLayer current)
        {
            super(current);
            this.start();
        }
    }
    class BSCLayer extends Layer
    {
        public void createNew()
        {
            this.stations.add(new BSC(this));
        }
        @Override
        public Station findMin(SMS sms) {
            Station s= super.findMin(sms);
            if(this.isPiled())this.createNew();
            return this.stations.get(this.stations.size()-1);
        }
        BSCLayer(Layer next)
        {
            super(next);
            this.stations=new ArrayList<>();
            this.stations.add(new BSC(this));
        }
    }
    abstract class Station extends Thread
    {
        final int number;
         long delay;
         Layer current;
         volatile boolean running=true;
         ConcurrentHashMap<SMS, Long> record;
        Station(Layer current)
        {
            this.current=current;
            this.number=this.current.stations.size()+1;
            this.record=new ConcurrentHashMap<>();
            this.delay=(long) (Math.random()*5000)+5000;
        }
        @Override
        public void run()
        {
            while (running)
            {

                    for(Map.Entry<SMS,Long> e: this.record.entrySet())
                        {
                            long currentTime=System.currentTimeMillis();
                            if(currentTime-e.getValue() >=this.delay)
                            {
                                this.passTo(this.current.next.findMin(e.getKey()),e.getKey());
                            }
                        }
            }
            this.current.removeStation(this);
        }
        synchronized void extremePass()
        {
            for(Map.Entry<SMS,Long> e: this.record.entrySet())
            {
                this.passTo(this.current.next.findMin(e.getKey()),e.getKey());
            }
        }
        synchronized void removeData(SMS sms)
        {
            this.record.remove(sms);
        }
        synchronized void addData(SMS sms, long timestamp)
        {
            this.record.put(sms,timestamp);
        }
        synchronized void passTo(VRD vrd,SMS sms)
        {
            if(vrd.record.containsKey(sms)) vrd.record.put(sms,vrd.record.get(sms)+1);
            else vrd.record.put(sms,1L);
            System.out.println("VRD number "+vrd.number+" received SMS");
        }
        synchronized void passTo(Station station,SMS sms)
    {
        station.addData(sms,System.currentTimeMillis());
            System.out.println("Station number "+station.number+" "+station.current.getClass()+" "+(station.current.next!=null?station.current.next.getClass():"")+" received SMS "+sms.message);
        this.removeData(sms);
    }
    }
    class VRDS extends Layer
        {
            VRDS()
            {
                super(null);
                this.stations =new ArrayList<>();
                this.stations.add(new VRD(this));
            }
            @Override
            public Station findMin(SMS sms)
            {
                return this.stations.get(sms.recipient-1);
            }
        }
    class VRD extends Station
    {
        private volatile boolean delition=false;
        @Override
        public void addData(SMS sms, long timestamp)
        {
            super.addData(sms, timestamp);
        }

        @Override
        public void run()
        {
            while (this.running)
            {
                while (this.delition)
                {
                    for(Map.Entry<SMS,Long> e: this.record.entrySet())
                    {
                        this.record.put(e.getKey(),0l);
                    }
                    try
                    {
                        Thread.sleep(this.delay);
                    }
                    catch (InterruptedException ignored){}
                }
            }
        }
        VRD(VRDS current)
        {
            super(current);
            this.start();
        }
    }
}
